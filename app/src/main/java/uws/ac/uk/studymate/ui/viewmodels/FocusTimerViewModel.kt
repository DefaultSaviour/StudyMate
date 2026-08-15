package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.AssignmentTask
import uws.ac.uk.studymate.data.repositories.AssignmentTaskRepo
import uws.ac.uk.studymate.data.repositories.FocusSessionRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.notifications.FocusTimerScheduler
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.FocusTimerEngine
import uws.ac.uk.studymate.util.FocusTimerEngine.Config
import uws.ac.uk.studymate.util.FocusTimerEngine.Phase
import uws.ac.uk.studymate.util.FocusTimerEngine.TimerState
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.Instant
import kotlin.math.ceil

/*//////////////////////
Focus / Pomodoro timer (0.9G).
Owns the wall-clock ticking around the pure FocusTimerEngine rules.

The countdown is timestamp-based: while running we remember when the current phase
*ends* and recompute the remaining seconds each tick — so rotation and
leaving/returning to the screen stay accurate (the snapshot is persisted to
SharedPreferences, using wall-clock so it survives the screen being destroyed).

A background WorkManager one-shot (FocusTimerScheduler) is kept in sync as the
minimised-app fallback for the phase-complete notification.
 *//////////////////////
class FocusTimerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS, Application.MODE_PRIVATE)

    // Study context (0.9J): the optional assignment being studied + its checklist.
    // The timer works fully without this — selecting an assignment is opt-in.
    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val taskRepo = AssignmentTaskRepo(db)
    private val focusRepo = FocusSessionRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _state = MutableLiveData<TimerState>()
    val state: LiveData<TimerState> = _state

    // Active assignments to choose from, the current selection, and its checklist.
    private val _assignments = MutableLiveData<List<Assignment>>()
    val assignments: LiveData<List<Assignment>> = _assignments

    private val _selectedAssignment = MutableLiveData<Assignment?>()
    val selectedAssignment: LiveData<Assignment?> = _selectedAssignment

    private val _tasks = MutableLiveData<List<AssignmentTask>>()
    val tasks: LiveData<List<AssignmentTask>> = _tasks

    private val _config = MutableLiveData<Config>()
    val config: LiveData<Config> = _config

    // One-shot signal that a phase just changed (Activity buzzes the device).
    // Holds the phase we advanced *into*; cleared via [consumePhaseEvent].
    private val _phaseEvent = MutableLiveData<Phase?>()
    val phaseEvent: LiveData<Phase?> = _phaseEvent

    private var cfg: Config = loadConfig()
    private var current: TimerState = FocusTimerEngine.initial(cfg)

    // When the current phase ends, in each clock domain. *Elapsed is monotonic
    // (immune to clock changes) and drives the live tick; *Wall survives process
    // death and drives restore.
    private var phaseEndElapsed: Long = 0L
    private var tickJob: Job? = null

    // Focused seconds accrued in the current FOCUS phase (0.9J) — flushed to a
    // Focus_Sessions row at each focus boundary / End, then reset. Breaks don't count.
    private var focusedAccumSeconds = 0

    init {
        _config.value = cfg
        restore()
    }

    // ── Public controls ──

    fun start() {
        if (current.phase == Phase.DONE) current = FocusTimerEngine.initial(cfg)
        if (current.running) return
        beginRunning(current.remainingSeconds)
    }

    fun pause() {
        if (!current.running) return
        val remaining = liveRemaining()
        stopTicking()
        current = current.copy(remainingSeconds = remaining, running = false)
        FocusTimerScheduler.cancel(getApplication())
        publish()
        persist()
    }

    fun reset() {
        // "End": log whatever focus time was accrued so far before clearing.
        if (current.phase == Phase.FOCUS) flushFocusedSeconds()
        stopTicking()
        FocusTimerScheduler.cancel(getApplication())
        current = FocusTimerEngine.initial(cfg)
        publish()
        persist()
    }

    // Jump straight to the next phase (counts the current one as finished).
    fun skipPhase() {
        // Skipping a focus phase logs the focus time accrued so far (not the skipped rest).
        if (current.phase == Phase.FOCUS) flushFocusedSeconds()
        val wasRunning = current.running
        val next = FocusTimerEngine.advance(current.copy(remainingSeconds = 0), cfg)
        stopTicking()
        if (wasRunning && next.phase != Phase.DONE) {
            current = next
            beginRunning(next.remainingSeconds)
        } else {
            current = next.copy(running = false)
            FocusTimerScheduler.cancel(getApplication())
            publish()
            persist()
        }
        _phaseEvent.value = next.phase
    }

    // Apply a preset / custom config. Only honoured while not running (the screen
    // disables the preset row mid-session).
    fun applyConfig(focusMinutes: Int, breakMinutes: Int, rounds: Int) {
        if (current.running) return
        cfg = Config(
            focusMinutes = focusMinutes.coerceIn(1, 180),
            breakMinutes = breakMinutes.coerceIn(1, 60),
            rounds = rounds.coerceIn(1, 12)
        )
        _config.value = cfg
        saveConfig()
        current = FocusTimerEngine.initial(cfg)
        publish()
        persist()
    }

    fun consumePhaseEvent() {
        _phaseEvent.value = null
    }

    // ── Study context (0.9J) ──

    // Load the active (not finished/past-due) assignments to choose from, restore the
    // last selection from prefs, and load its checklist. Called from the Activity.
    fun loadStudyContext() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: run {
                _assignments.postValue(emptyList())
                _selectedAssignment.postValue(null)
                _tasks.postValue(emptyList())
                return@launch
            }
            val active = db.assignmentDao().getAssignments(session.userId)
                .filter { !AssignmentDateTimeUtils.isComplete(it.completedAt, it.dueDate) }
                .sortedBy { it.title.lowercase() }
            _assignments.postValue(active)

            // Restore the remembered selection, but only if it's still an active one.
            val savedId = prefs.getInt(KEY_SELECTED_ASSIGNMENT, -1).takeIf { it >= 0 }
            val selected = active.firstOrNull { it.id == savedId }
            if (selected == null && savedId != null) {
                prefs.edit().remove(KEY_SELECTED_ASSIGNMENT).apply()
            }
            _selectedAssignment.postValue(selected)
            _tasks.postValue(selected?.let { taskRepo.getForAssignment(it.id) } ?: emptyList())
        }
    }

    // Pick an assignment to study (or null to clear). Persists so it survives leave/return.
    fun selectAssignment(assignmentId: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (assignmentId == null) {
                prefs.edit().remove(KEY_SELECTED_ASSIGNMENT).apply()
                _selectedAssignment.postValue(null)
                _tasks.postValue(emptyList())
                return@launch
            }
            prefs.edit().putInt(KEY_SELECTED_ASSIGNMENT, assignmentId).apply()
            val assignment = db.assignmentDao().getById(assignmentId)
            _selectedAssignment.postValue(assignment)
            _tasks.postValue(assignment?.let { taskRepo.getForAssignment(it.id) } ?: emptyList())
        }
    }

    // Tick / untick a checklist item from the focus screen (writes the shared table),
    // then reload the checklist.
    fun toggleFocusTask(task: AssignmentTask) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepo.setDone(task.id, !task.isDone)
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            _tasks.postValue(taskRepo.getForAssignment(task.assignmentId))
        }
    }

    // Delete a checklist item from the focus screen, then reload the checklist.
    fun deleteFocusTask(task: AssignmentTask) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepo.delete(task)
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            _tasks.postValue(taskRepo.getForAssignment(task.assignmentId))
        }
    }

    // Add a checklist item to the currently selected assignment from the focus
    // screen (so the user can jot down a step mid-session). No-op if nothing's
    // selected or the text is blank.
    fun addTaskToSelected(rawText: String) {
        val text = TextSanitizer.singleLine(rawText)
        if (text.isEmpty()) return
        val assignmentId = prefs.getInt(KEY_SELECTED_ASSIGNMENT, -1).takeIf { it >= 0 } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: return@launch
            taskRepo.add(session.userId, assignmentId, text, Instant.now().toString())
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            _tasks.postValue(taskRepo.getForAssignment(assignmentId))
        }
    }

    // Log the accrued focus time as a Focus_Sessions row (against the selected
    // assignment, if any) and reset the accumulator. No-op for a zero block.
    private fun flushFocusedSeconds() {
        val seconds = focusedAccumSeconds
        focusedAccumSeconds = 0
        if (seconds <= 0) return
        val assignmentId = prefs.getInt(KEY_SELECTED_ASSIGNMENT, -1).takeIf { it >= 0 }
        val endedAt = Instant.now().toString()
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser() ?: return@launch
            focusRepo.log(session.userId, assignmentId, seconds, endedAt)
        }
    }

    // ── Internals ──

    private fun beginRunning(remainingSeconds: Int) {
        current = current.copy(remainingSeconds = remainingSeconds, running = true)
        val remainingMs = remainingSeconds * 1000L
        phaseEndElapsed = SystemClock.elapsedRealtime() + remainingMs
        publish()
        persist()
        FocusTimerScheduler.schedule(getApplication(), remainingMs, messageForPhaseEnd(current))
        startTicking()
    }

    private fun startTicking() {
        stopTicking()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = liveRemaining()
                if (remaining <= 0) {
                    onPhaseEnded()
                    if (current.phase == Phase.DONE) break
                } else if (remaining != current.remainingSeconds) {
                    // Count the elapsed whole second(s) toward focus time (FOCUS only).
                    if (current.phase == Phase.FOCUS) {
                        focusedAccumSeconds += (current.remainingSeconds - remaining).coerceAtLeast(0)
                    }
                    // Persist on every whole-second change so a sudden close/kill
                    // leaves the latest remaining behind — the timer "stops" there.
                    current = current.copy(remainingSeconds = remaining, running = true)
                    publish()
                    persist()
                }
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun onPhaseEnded() {
        // A FOCUS phase just completed naturally — count its final second(s) and log it.
        if (current.phase == Phase.FOCUS) {
            focusedAccumSeconds += current.remainingSeconds.coerceAtLeast(0)
            flushFocusedSeconds()
        }
        val next = FocusTimerEngine.advance(current, cfg)
        current = next
        _phaseEvent.postValue(next.phase)
        if (next.phase == Phase.DONE) {
            stopTicking()
            FocusTimerScheduler.cancel(getApplication())
            publish()
            persist()
            return
        }
        // Continue into the next phase.
        val remainingMs = next.remainingSeconds * 1000L
        phaseEndElapsed = SystemClock.elapsedRealtime() + remainingMs
        publish()
        persist()
        FocusTimerScheduler.schedule(getApplication(), remainingMs, messageForPhaseEnd(next))
    }

    // Seconds left in the current phase, from the monotonic clock (rounded up so
    // a full phase reads its nominal length on the first tick).
    private fun liveRemaining(): Int {
        val ms = phaseEndElapsed - SystemClock.elapsedRealtime()
        return ceil(ms / 1000.0).toInt().coerceAtLeast(0)
    }

    private fun messageForPhaseEnd(state: TimerState): String {
        val app = getApplication<Application>()
        return when {
            state.phase == Phase.FOCUS -> app.getString(R.string.focus_timer_focus_done)
            state.phase == Phase.BREAK && state.round >= cfg.rounds ->
                app.getString(R.string.focus_timer_session_done)
            else -> app.getString(R.string.focus_timer_break_done)
        }
    }

    private fun publish() {
        _state.value = current
    }

    // ── Persistence ──

    // Cold start (first launch, or after the app was closed / the process was
    // killed). The timer "stops on close": whether it was running or paused when
    // the process ended, we come back **paused** at the last persisted remaining —
    // never catching up. (A mere background/return keeps the same VM ticking, so
    // this isn't called then.)
    private fun restore() {
        if (!prefs.contains(KEY_PHASE)) {
            current = FocusTimerEngine.initial(cfg)
            publish()
            return
        }
        val phase = runCatching { Phase.valueOf(prefs.getString(KEY_PHASE, Phase.FOCUS.name)!!) }
            .getOrDefault(Phase.FOCUS)
        val round = prefs.getInt(KEY_ROUND, 1)
        val remaining = prefs.getInt(KEY_REMAINING, FocusTimerEngine.phaseDurationSeconds(phase, cfg))
        val wasRunning = prefs.getBoolean(KEY_RUNNING, false)

        // If it was running when the process died, a phase-end notification may
        // still be queued — drop it, since the timer is now stopped.
        if (wasRunning) FocusTimerScheduler.cancel(getApplication())

        current = TimerState(phase, round, remaining, running = false)
        publish()
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY_PHASE, current.phase.name)
            .putInt(KEY_ROUND, current.round)
            .putInt(KEY_REMAINING, current.remainingSeconds)
            .putBoolean(KEY_RUNNING, current.running)
            .apply()
    }

    private fun loadConfig(): Config = Config(
        focusMinutes = prefs.getInt(KEY_FOCUS_MIN, DEFAULT_FOCUS),
        breakMinutes = prefs.getInt(KEY_BREAK_MIN, DEFAULT_BREAK),
        rounds = prefs.getInt(KEY_ROUNDS, DEFAULT_ROUNDS)
    )

    private fun saveConfig() {
        prefs.edit()
            .putInt(KEY_FOCUS_MIN, cfg.focusMinutes)
            .putInt(KEY_BREAK_MIN, cfg.breakMinutes)
            .putInt(KEY_ROUNDS, cfg.rounds)
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        stopTicking()
    }

    companion object {
        private const val PREFS = "focus_timer"
        private const val TICK_MS = 250L

        const val DEFAULT_FOCUS = 25
        const val DEFAULT_BREAK = 5
        const val DEFAULT_ROUNDS = 3

        private const val KEY_FOCUS_MIN = "focus_min"
        private const val KEY_BREAK_MIN = "break_min"
        private const val KEY_ROUNDS = "rounds"
        private const val KEY_PHASE = "phase"
        private const val KEY_ROUND = "round"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_RUNNING = "running"
        private const val KEY_SELECTED_ASSIGNMENT = "selected_assignment"
    }
}

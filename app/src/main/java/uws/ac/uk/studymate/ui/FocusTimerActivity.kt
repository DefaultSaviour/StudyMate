package uws.ac.uk.studymate.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.ui.viewmodels.FocusTimerViewModel
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.ColorUtils
import uws.ac.uk.studymate.util.FocusTimerEngine
import uws.ac.uk.studymate.util.FocusTimerEngine.Config
import uws.ac.uk.studymate.util.FocusTimerEngine.Phase

/*//////////////////////
Focus / Pomodoro timer screen (0.9G).

Wood-glass screen that runs a focus ⇄ break Pomodoro session. The timer logic
lives in FocusTimerViewModel (timestamp-based, survives rotation / leave-return);
this Activity only renders state, wires the controls, buzzes on phase changes and
collects a custom config.
 *//////////////////////
class FocusTimerActivity : AppCompatActivity() {

    private lateinit var vm: FocusTimerViewModel

    private lateinit var card: MaterialCardView
    private lateinit var phaseLabel: TextView
    private lateinit var roundText: TextView
    private lateinit var countdownText: TextView
    private lateinit var countdownProgress: CircularProgressView
    private lateinit var startPauseBtn: MaterialButton
    private lateinit var skipBtn: MaterialButton
    private lateinit var endBtn: MaterialButton
    private lateinit var studyBox: TextView
    private lateinit var breakBox: TextView
    private lateinit var roundsBox: TextView

    // Study context (0.9J): assignment selector + checklist.
    private lateinit var assignmentSelector: LinearLayout
    private lateinit var assignmentSelectorIcon: ImageView
    private lateinit var assignmentSelectorText: TextView
    private lateinit var checklistBlock: View
    private lateinit var checklistCount: TextView
    private lateinit var checklistEmpty: TextView
    private lateinit var checklistRecycler: RecyclerView
    private lateinit var checklistAddBtn: MaterialButton
    private lateinit var focusTaskAdapter: TaskListAdapter
    private var pickerAssignments: List<Assignment> = emptyList()

    private var lastConfig: Config = Config(
        FocusTimerViewModel.DEFAULT_FOCUS,
        FocusTimerViewModel.DEFAULT_BREAK,
        FocusTimerViewModel.DEFAULT_ROUNDS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_timer)
        uws.ac.uk.studymate.util.OrientationLock.apply(this)

        vm = ViewModelProvider(this)[FocusTimerViewModel::class.java]

        bindViews()
        setupClicks()
        setupWindowInsets()

        uws.ac.uk.studymate.util.OrbField.scatter(
            findViewById(R.id.focusCard),
            listOf(findViewById(R.id.backBtn))
        )

        // Entrance: card slides up (shared util/Entrance, pop-free). 1.1
        uws.ac.uk.studymate.util.Entrance.play(card)

        vm.config.observe(this) { cfg ->
            lastConfig = cfg
            studyBox.text = cfg.focusMinutes.toString()
            breakBox.text = cfg.breakMinutes.toString()
            roundsBox.text = cfg.rounds.toString()
        }
        vm.state.observe(this) { render(it) }
        vm.phaseEvent.observe(this) { phase ->
            if (phase == null) return@observe
            buzz()
            if (phase == Phase.DONE) {
                Toast.makeText(this, R.string.focus_timer_session_done, Toast.LENGTH_LONG).show()
            }
            vm.consumePhaseEvent()
        }

        vm.assignments.observe(this) { pickerAssignments = it }
        vm.selectedAssignment.observe(this) { renderSelectedAssignment(it) }
        vm.tasks.observe(this) { renderTasks(it) }
    }

    override fun onResume() {
        super.onResume()
        // Refresh assignments + checklist in case they were edited on the Assignments screen.
        vm.loadStudyContext()
    }

    private fun bindViews() {
        card = findViewById(R.id.focusCard)
        phaseLabel = findViewById(R.id.phaseLabel)
        roundText = findViewById(R.id.roundText)
        countdownText = findViewById(R.id.countdownText)
        countdownProgress = findViewById(R.id.countdownProgress)
        startPauseBtn = findViewById(R.id.startPauseBtn)
        skipBtn = findViewById(R.id.skipBtn)
        endBtn = findViewById(R.id.endBtn)
        studyBox = findViewById(R.id.studyBox)
        breakBox = findViewById(R.id.breakBox)
        roundsBox = findViewById(R.id.roundsBox)

        assignmentSelector = findViewById(R.id.assignmentSelector)
        assignmentSelectorIcon = findViewById(R.id.assignmentSelectorIcon)
        assignmentSelectorText = findViewById(R.id.assignmentSelectorText)
        checklistBlock = findViewById(R.id.focusChecklistBlock)
        checklistCount = findViewById(R.id.focusChecklistCount)
        checklistEmpty = findViewById(R.id.focusChecklistEmpty)
        checklistRecycler = findViewById(R.id.focusChecklistRecycler)
        checklistAddBtn = findViewById(R.id.focusChecklistAddBtn)

        focusTaskAdapter = TaskListAdapter(
            items = emptyList(),
            onToggle = { vm.toggleFocusTask(it) },
            onDelete = { confirmDeleteFocusTask(it) },
            onClick = { showTaskText(it) }
        )
        checklistRecycler.layoutManager = LinearLayoutManager(this)
        checklistRecycler.adapter = focusTaskAdapter
    }

    private fun setupClicks() {
        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener { finish() }
        startPauseBtn.setOnClickListener {
            val running = vm.state.value?.running == true
            if (running) vm.pause() else vm.start()
        }
        skipBtn.setOnClickListener { vm.skipPhase() }
        endBtn.setOnClickListener { vm.reset() }
        studyBox.setOnClickListener { editStudy() }
        breakBox.setOnClickListener { editBreak() }
        roundsBox.setOnClickListener { editRounds() }
        assignmentSelector.setOnClickListener { showAssignmentPicker() }
        checklistAddBtn.setOnClickListener { showAddTaskDialog() }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(card) { _, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val base = (4 * resources.displayMetrics.density).toInt()
            findViewById<View>(R.id.focusScrollView).setPadding(0, 0, 0, navBar + base)
            insets
        }
    }

    private fun render(state: FocusTimerEngine.TimerState) {
        countdownText.text = formatTime(state.remainingSeconds)

        phaseLabel.text = when (state.phase) {
            Phase.FOCUS -> getString(R.string.focus_phase_focus)
            Phase.BREAK -> getString(R.string.focus_phase_break)
            Phase.DONE -> getString(R.string.focus_phase_done)
        }

        roundText.text = if (state.phase == Phase.DONE) {
            getString(R.string.focus_session_complete)
        } else {
            getString(R.string.focus_round_format, state.round, lastConfig.rounds)
        }

        // Start / Pause / Resume / Start again
        startPauseBtn.text = when {
            state.running -> getString(R.string.focus_pause)
            state.phase == Phase.DONE -> getString(R.string.focus_start_again)
            state.remainingSeconds < FocusTimerEngine.phaseDurationSeconds(state.phase, lastConfig) ->
                getString(R.string.focus_resume)
            else -> getString(R.string.focus_start)
        }

        // "End round" during focus, "End break" during a break.
        skipBtn.text = getString(
            if (state.phase == Phase.BREAK) R.string.focus_end_break else R.string.focus_end_round
        )
        // "End round/break" and "End" are only available once the session is
        // actually under way — not on the fresh idle screen, and not when finished.
        val atIdleStart = !state.running &&
            state.phase == Phase.FOCUS &&
            state.round == 1 &&
            state.remainingSeconds >= FocusTimerEngine.phaseDurationSeconds(Phase.FOCUS, lastConfig)
        val started = state.phase != Phase.DONE && !atIdleStart
        skipBtn.isEnabled = started
        skipBtn.alpha = if (started) 1f else 0.45f
        endBtn.isEnabled = started
        endBtn.alpha = if (started) 1f else 0.45f

        // Config boxes can only change when the session isn't in progress — i.e.
        // at the idle start screen or once it's finished, never while running OR paused.
        val configEnabled = !started
        listOf(studyBox, breakBox, roundsBox).forEach {
            it.isEnabled = configEnabled
            it.isClickable = configEnabled
            it.alpha = if (configEnabled) 1f else 0.45f
        }

        // Drive the circular progress ring: full at the start of a phase, empty at the end.
        val phaseDuration = FocusTimerEngine.phaseDurationSeconds(state.phase, lastConfig)
        val progressFraction = if (phaseDuration > 0) {
            (state.remainingSeconds.toFloat() / phaseDuration).coerceIn(0f, 1f)
        } else 1f
        countdownProgress.setProgress(progressFraction, animate = state.running)
    }

    // ─────────────────── Study context (0.9J) ───────────────────

    // Pick an assignment to study (or clear it). Themed list dialog of active assignments.
    private fun showAssignmentPicker() {
        val assignments = pickerAssignments
        // "None" first, then the assignment titles.
        val labels = buildList {
            add(getString(R.string.focus_clear_assignment))
            addAll(assignments.map { it.title })
        }.toTypedArray()
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(R.string.focus_pick_assignment_title)
            .setItems(labels) { _, which ->
                if (which == 0) vm.selectAssignment(null)
                else vm.selectAssignment(assignments[which - 1].id)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    // Add a checklist item to the selected assignment without leaving the timer.
    private fun showAddTaskDialog() {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setHint(R.string.focus_choose_assignment_hint)
            setTextColor(getColor(R.color.surface))
            setHintTextColor(getColor(R.color.gold_light))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(R.string.focus_add_task_title)
            .setView(container)
            .setPositiveButton(R.string.focus_add_task_add) { _, _ ->
                vm.addTaskToSelected(input.text.toString())
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    // Show one checklist item's full text (rows truncate to two lines).
    private fun showTaskText(task: uws.ac.uk.studymate.data.entities.AssignmentTask) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(if (task.isDone) "Checklist item (done)" else "Checklist item")
            .setMessage(task.text)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmDeleteFocusTask(task: uws.ac.uk.studymate.data.entities.AssignmentTask) {
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle("Delete checklist item")
            .setMessage("Delete \"${task.text}\"?")
            .setPositiveButton("Delete") { _, _ -> vm.deleteFocusTask(task) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderSelectedAssignment(assignment: Assignment?) {
        if (assignment == null) {
            assignmentSelectorText.text = getString(R.string.focus_choose_assignment)
            assignmentSelectorIcon.visibility = View.GONE
            checklistBlock.visibility = View.GONE
            return
        }
        assignmentSelectorText.text = assignment.title
        assignmentSelectorIcon.setImageResource(AssignmentIcons.drawableForKey(assignment.icon))
        assignmentSelectorIcon.setColorFilter(ColorUtils.parseOrDefault(assignment.color))
        assignmentSelectorIcon.visibility = View.VISIBLE
        checklistBlock.visibility = View.VISIBLE
    }

    private fun renderTasks(tasks: List<uws.ac.uk.studymate.data.entities.AssignmentTask>) {
        // Only meaningful when an assignment is selected; the block is hidden otherwise.
        focusTaskAdapter.submit(tasks)
        val done = tasks.count { it.isDone }
        checklistCount.text = getString(R.string.focus_checklist_count, done, tasks.size)
        val isEmpty = tasks.isEmpty()
        checklistEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        checklistRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // Tap a config box → edit just that value, keeping the other two.
    private fun editStudy() = showValueDialog(R.string.focus_edit_study, lastConfig.focusMinutes) { v ->
        vm.applyConfig(v, lastConfig.breakMinutes, lastConfig.rounds)
    }

    private fun editBreak() = showValueDialog(R.string.focus_edit_break, lastConfig.breakMinutes) { v ->
        vm.applyConfig(lastConfig.focusMinutes, v, lastConfig.rounds)
    }

    private fun editRounds() = showValueDialog(R.string.focus_edit_rounds, lastConfig.rounds) { v ->
        vm.applyConfig(lastConfig.focusMinutes, lastConfig.breakMinutes, v)
    }

    private fun showValueDialog(titleRes: Int, current: Int, onSet: (Int) -> Unit) {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
            setTextColor(getColor(R.color.surface))
            setHintTextColor(getColor(R.color.gold_light))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this, R.style.Theme_StudyMate_AlertDialog)
            .setTitle(titleRes)
            .setView(container)
            .setPositiveButton(R.string.focus_edit_apply) { _, _ ->
                input.text.toString().toIntOrNull()?.let { onSet(it) }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun formatTime(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    private fun buzz() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

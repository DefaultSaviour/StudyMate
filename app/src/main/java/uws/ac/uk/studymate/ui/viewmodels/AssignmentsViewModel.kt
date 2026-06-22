package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.AssignmentTask
import uws.ac.uk.studymate.data.repositories.AssignmentRepo
import uws.ac.uk.studymate.data.repositories.AssignmentTaskRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.notifications.AssignmentReminderScheduler
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.Instant
import java.time.LocalDateTime
/*//////////////////////
Coded by Jamie Coleman
05/04/26
consolidated 18/04/26 — list + add + edit + delete in one ViewModel for the wood-glass redesign
merged 17/06/26 — Subject folded in; an assignment now carries its own colour
 *//////////////////////
// One named colour the add/edit form can show as a swatch (was on Subjects).
data class ColorChoice(
    val label: String,
    val hex: String
)

data class AssignmentsItem(
    val assignment: Assignment,
    val dueAt: LocalDateTime,
    val colorHex: String?,
    val iconKey: String,
    val isCompleted: Boolean,
    val taskDone: Int = 0,    // Checklist progress (0.9J): items ticked …
    val taskTotal: Int = 0    // … out of total. 0/0 = no checklist on this assignment.
)

// The checklist panel's state for one assignment (0.9J).
data class ChecklistState(
    val assignment: Assignment,
    val tasks: List<AssignmentTask>
)

data class AssignmentsSummary(
    val titleText: String,
    val items: List<AssignmentsItem>,
    val colorChoices: List<ColorChoice>
)

class AssignmentsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val assignmentRepo = AssignmentRepo(db)
    private val taskRepo = AssignmentTaskRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _assignmentsSummary = MutableLiveData<AssignmentsSummary>()
    val assignmentsSummary: LiveData<AssignmentsSummary> = _assignmentsSummary

    private val _checklist = MutableLiveData<ChecklistState>()
    val checklist: LiveData<ChecklistState> = _checklist

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadAssignments() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val assignments = db.assignmentDao().getAssignments(session.userId)
            val progress = db.assignmentTaskDao().progressForUser(session.userId)
                .associateBy { it.assignmentId }
            val summary = AssignmentsSummary(
                titleText = "Assignments",
                items = buildAssignmentItems(assignments, progress),
                colorChoices = buildColorChoices()
            )
            _assignmentsSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    fun addAssignment(title: String, colorHex: String?, dueDate: String?, iconKey: String?) {
        val trimmedTitle = TextSanitizer.singleLine(title)
        if (trimmedTitle.isEmpty()) {
            _message.value = "Enter an assignment name"
            return
        }
        if (colorHex.isNullOrBlank()) {
            _message.value = "Choose a colour"
            return
        }
        if (dueDate.isNullOrBlank()) {
            _message.value = "Choose a due date"
            return
        }

        val savedIconKey = AssignmentIcons.optionForKey(iconKey).key

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val toInsert = Assignment(
                userId = session.userId,
                title = trimmedTitle,
                color = colorHex,
                dueDate = dueDate,
                icon = savedIconKey
            )
            val newId = assignmentRepo.addAssignment(toInsert).toInt()
            AssignmentReminderScheduler.scheduleForAssignment(
                getApplication(),
                toInsert.copy(id = newId),
                session.value
            )
            _message.postValue("Assignment added")
            loadAssignments()
        }
    }

    fun updateAssignment(
        original: Assignment,
        title: String,
        colorHex: String?,
        dueDate: String?,
        iconKey: String?
    ) {
        val trimmedTitle = TextSanitizer.singleLine(title)
        if (trimmedTitle.isEmpty()) {
            _message.value = "Enter an assignment name"
            return
        }
        if (colorHex.isNullOrBlank()) {
            _message.value = "Choose a colour"
            return
        }
        if (dueDate.isNullOrBlank()) {
            _message.value = "Choose a due date"
            return
        }

        val savedIconKey = AssignmentIcons.optionForKey(iconKey).key

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val updated = original.copy(
                title = trimmedTitle,
                color = colorHex,
                dueDate = dueDate,
                icon = savedIconKey
            )
            assignmentRepo.updateAssignment(updated)
            AssignmentReminderScheduler.scheduleForAssignment(
                getApplication(),
                updated,
                session.value
            )
            _message.postValue("Assignment updated")
            loadAssignments()
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            assignmentRepo.deleteAssignment(assignment)
            AssignmentReminderScheduler.cancelForAssignment(getApplication(), assignment.id)
            _message.postValue("Assignment deleted")
            loadAssignments()
        }
    }

    // Toggle an assignment's done state. Completing it stamps completed_at and
    // cancels its reminders (no nagging about finished work); un-completing it
    // clears the stamp and re-schedules the reminders.
    fun toggleComplete(item: AssignmentsItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            val markDone = item.assignment.completedAt == null
            val completedAt = if (markDone) Instant.now().toString() else null
            assignmentRepo.setCompleted(item.assignment, completedAt)
            if (markDone) {
                AssignmentReminderScheduler.cancelForAssignment(getApplication(), item.assignment.id)
            } else {
                AssignmentReminderScheduler.scheduleForAssignment(
                    getApplication(),
                    item.assignment.copy(completedAt = null),
                    session.value
                )
            }
            _message.postValue(if (markDone) "Marked done" else "Marked not done")
            loadAssignments()
        }
    }

    // ─────────────────── Checklist (0.9J) ───────────────────

    // Load one assignment's checklist for the checklist panel.
    fun loadChecklist(assignmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            val assignment = db.assignmentDao().getById(assignmentId) ?: return@launch
            _checklist.postValue(ChecklistState(assignment, taskRepo.getForAssignment(assignmentId)))
        }
    }

    // Append a new checklist item (sanitised; blank ignored), then reload the panel.
    fun addTask(assignmentId: Int, rawText: String) {
        val text = TextSanitizer.singleLine(rawText)
        if (text.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            taskRepo.add(session.userId, assignmentId, text, Instant.now().toString())
            _checklist.postValue(
                ChecklistState(
                    db.assignmentDao().getById(assignmentId) ?: return@launch,
                    taskRepo.getForAssignment(assignmentId)
                )
            )
        }
    }

    // Tick / untick an item, then reload the panel.
    fun toggleTask(task: AssignmentTask) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepo.setDone(task.id, !task.isDone)
            _checklist.postValue(
                ChecklistState(
                    db.assignmentDao().getById(task.assignmentId) ?: return@launch,
                    taskRepo.getForAssignment(task.assignmentId)
                )
            )
        }
    }

    // Delete an item, then reload the panel.
    fun deleteTask(task: AssignmentTask) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepo.delete(task)
            _checklist.postValue(
                ChecklistState(
                    db.assignmentDao().getById(task.assignmentId) ?: return@launch,
                    taskRepo.getForAssignment(task.assignmentId)
                )
            )
        }
    }

    private fun buildAssignmentItems(
        assignments: List<Assignment>,
        progress: Map<Int, uws.ac.uk.studymate.data.dao.TaskProgress>
    ): List<AssignmentsItem> {
        val now = LocalDateTime.now()
        return assignments
            .mapNotNull { assignment ->
                val dueAt = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                val taskProgress = progress[assignment.id]

                // No "overdue": a past-due assignment is shown as completed, not hidden,
                // so the user can still see and delete it.
                AssignmentsItem(
                    assignment = assignment,
                    dueAt = dueAt,
                    colorHex = assignment.color,
                    iconKey = assignment.icon,
                    isCompleted = AssignmentDateTimeUtils.isComplete(
                        assignment.completedAt, assignment.dueDate, now
                    ),
                    taskDone = taskProgress?.done ?: 0,
                    taskTotal = taskProgress?.total ?: 0
                )
            }
            .sortedWith(
                // Completed assignments sink to the bottom; otherwise by due date.
                compareBy<AssignmentsItem> { it.isCompleted }
                    .thenBy { it.dueAt }
                    .thenBy { it.assignment.title.lowercase() }
            )
    }

    // The small fixed list of colour names the add/edit form can use (was Subjects).
    private fun buildColorChoices(): List<ColorChoice> {
        return listOf(
            ColorChoice(label = "Purple", hex = "#A855F7"),
            ColorChoice(label = "Pink", hex = "#EC4899"),
            ColorChoice(label = "Blue", hex = "#3B82F6"),
            ColorChoice(label = "Yellow", hex = "#EAB308"),
            ColorChoice(label = "Green", hex = "#22C55E"),
            ColorChoice(label = "Orange", hex = "#F97316")
        )
    }
}

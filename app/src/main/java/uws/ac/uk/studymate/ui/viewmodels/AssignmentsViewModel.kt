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
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.AssignmentRepo
import uws.ac.uk.studymate.data.repositories.SubjectRepo
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
 *//////////////////////
data class AssignmentsItem(
    val assignment: Assignment,
    val dueAt: LocalDateTime,
    val subjectName: String,
    val subjectColorHex: String?,
    val iconKey: String,
    val isCompleted: Boolean
)

data class AssignmentsSummary(
    val titleText: String,
    val items: List<AssignmentsItem>,
    val subjects: List<Subject>
)

class AssignmentsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val subjectRepo = SubjectRepo(db)
    private val assignmentRepo = AssignmentRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _assignmentsSummary = MutableLiveData<AssignmentsSummary>()
    val assignmentsSummary: LiveData<AssignmentsSummary> = _assignmentsSummary

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

            val userId = session.userId
            val user = session.value

            val assignments = db.assignmentDao().getAssignments(userId)
            val subjects = subjectRepo.getSubjects(userId).sortedBy { it.name.lowercase() }
            val subjectsById = subjects.associateBy { it.id }
            val summary = AssignmentsSummary(
                titleText = "Assignments",
                items = buildUpcomingAssignments(assignments, subjectsById),
                subjects = subjects
            )
            _assignmentsSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    fun addAssignment(title: String, subject: Subject?, dueDate: String?, iconKey: String?) {
        val trimmedTitle = TextSanitizer.singleLine(title)
        if (trimmedTitle.isEmpty()) {
            _message.value = "Enter an assignment title"
            return
        }
        if (subject == null) {
            _message.value = "Choose a subject first"
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
                subjectId = subject.id,
                title = trimmedTitle,
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
        subject: Subject?,
        dueDate: String?,
        iconKey: String?
    ) {
        val trimmedTitle = TextSanitizer.singleLine(title)
        if (trimmedTitle.isEmpty()) {
            _message.value = "Enter an assignment title"
            return
        }
        if (subject == null) {
            _message.value = "Choose a subject first"
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
                subjectId = subject.id,
                title = trimmedTitle,
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

    private fun buildUpcomingAssignments(
        assignments: List<Assignment>,
        subjectsById: Map<Int, Subject>
    ): List<AssignmentsItem> {
        val now = LocalDateTime.now()
        return assignments
            .mapNotNull { assignment ->
                val dueAt = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                if (dueAt.isBefore(now)) return@mapNotNull null

                val subject = subjectsById[assignment.subjectId]
                AssignmentsItem(
                    assignment = assignment,
                    dueAt = dueAt,
                    subjectName = subject?.name ?: "Unknown subject",
                    subjectColorHex = subject?.color,
                    iconKey = assignment.icon,
                    isCompleted = assignment.completedAt != null
                )
            }
            .sortedWith(
                // Completed assignments sink to the bottom; otherwise by due date.
                compareBy<AssignmentsItem> { it.isCompleted }
                    .thenBy { it.dueAt }
                    .thenBy { it.subjectName.lowercase() }
                    .thenBy { it.assignment.title.lowercase() }
            )
    }
}

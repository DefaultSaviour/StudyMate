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
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDateTime
/*//////////////////////
Coded by Jamie Coleman
05/04/26
fixed 08/04/26
 *//////////////////////
// Holds the text that the assignments screen needs to display.
data class AssignmentsItem(
    val title: String,
    val dueAt: LocalDateTime,
    val subjectName: String,
    val subjectColorHex: String?,
    val iconKey: String
)

data class AssignmentsSummary(
    val titleText: String,
    val items: List<AssignmentsItem>
)

class AssignmentsViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's assignments.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the shared session resolver so login validation stays consistent with other screens.
    private val sessionResolver = SessionUserResolver(application, repo)

    // This private value stores the latest assignments text.
    // It is mutable here so only the ViewModel can change it.
    private val _assignmentsSummary = MutableLiveData<AssignmentsSummary>()

    // This public version lets the UI observe the latest assignments data.
    val assignmentsSummary: LiveData<AssignmentsSummary> = _assignmentsSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadAssignments() {
        // Run the database work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {

            // Stop early when there is no valid logged-in user.
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val userId = session.userId
            val user = session.value

            // Load the user's assignments and turn them into assignments text.
            val assignments = db.assignmentDao().getAssignments(userId)
            val subjectsById = db.subjectDao().getSubjects(userId).associateBy { it.id }
            val summary = AssignmentsSummary(
                titleText = "Assignments for ${user.name}",
                items = buildUpcomingAssignments(assignments, subjectsById)
            )

            // Send the finished assignments data back to the UI.
            _assignmentsSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    // Keep only upcoming assignments and sort them so the nearest due work appears first.
    private fun buildUpcomingAssignments(
        assignments: List<Assignment>,
        subjectsById: Map<Int, Subject>
    ): List<AssignmentsItem> {
        val now = LocalDateTime.now()
        return assignments
            .mapNotNull { assignment ->
                val dueAt = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                if (dueAt.isBefore(now)) {
                    return@mapNotNull null
                }

                val subject = subjectsById[assignment.subjectId]
                AssignmentsItem(
                    title = assignment.title,
                    dueAt = dueAt,
                    subjectName = subject?.name ?: "Unknown subject",
                    subjectColorHex = subject?.color,
                    iconKey = assignment.icon
                )
            }
            .sortedWith(
                compareBy<AssignmentsItem> { it.dueAt }
                    .thenBy { it.subjectName.lowercase() }
                    .thenBy { it.title.lowercase() }
            )
    }
}



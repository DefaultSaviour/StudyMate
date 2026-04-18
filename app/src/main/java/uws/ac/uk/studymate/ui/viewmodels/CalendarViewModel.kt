package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDate
import java.time.LocalDateTime
/*//////////////////////
Coded by Jamie Coleman
06/04/26
updated 07/04/26
updated 09/04/26
 *//////////////////////
// Holds one assignment that should appear inside a calendar day cell.
data class CalendarAssignmentEntry(
    val subjectName: String,
    val assignmentTitle: String,
    val dueAt: LocalDateTime,
    val subjectColorHex: String?,
    val iconKey: String
)

// Holds the data that the calendar screen needs to display.
data class CalendarSummary(
    val titleText: String,
    val entriesByDate: Map<LocalDate, List<CalendarAssignmentEntry>>
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's assignments.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the shared session resolver so login validation stays consistent with other screens.
    private val sessionResolver = SessionUserResolver(application, repo)

    // This private value stores the latest calendar data.
    // It is mutable here so only the ViewModel can change it.
    private val _calendarSummary = MutableLiveData<CalendarSummary>()

    // This public version lets the UI observe the latest calendar data.
    val calendarSummary: LiveData<CalendarSummary> = _calendarSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadCalendar() {
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

            // Load the assignments and the subject colors that the calendar needs.
            val assignments = db.assignmentDao().getAssignments(userId)
            val subjectsById = db.subjectDao().getSubjects(userId).associateBy { it.id }

            val entriesByDate = assignments
                .mapNotNull { assignment ->
                    val dueAt = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                    val subject = subjectsById[assignment.subjectId]
                    CalendarAssignmentEntry(
                        subjectName = subject?.name ?: "Unknown subject",
                        assignmentTitle = assignment.title,
                        dueAt = dueAt,
                        subjectColorHex = subject?.color,
                        iconKey = assignment.icon
                    )
                }
                .sortedWith(
                    compareBy<CalendarAssignmentEntry> { it.dueAt }
                        .thenBy { it.subjectName.lowercase() }
                        .thenBy { it.assignmentTitle.lowercase() }
                )
                .groupBy { it.dueAt.toLocalDate() }

            // Send the finished calendar data back to the UI.
            _calendarSummary.postValue(
                CalendarSummary(
                    titleText = "Calendar for ${user.name}",
                    entriesByDate = entriesByDate
                )
            )
            _sessionExpired.postValue(false)
        }
    }
}

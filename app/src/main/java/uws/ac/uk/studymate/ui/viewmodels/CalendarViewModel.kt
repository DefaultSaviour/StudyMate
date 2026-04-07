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
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

// Holds the text that the calendar screen needs to display.
data class CalendarSummary(
    val titleText: String,
    val itemsText: String
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's assignments.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the session manager so this screen always reads data for the logged-in user.
    private val sessionManager = SessionManager(application)

    // This private value stores the latest calendar text.
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

            // Stop early when there is no logged-in user saved in the session.
            val userId = sessionManager.getLoggedInUserId()
            if (userId == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            // Load the user and end the session if their account no longer exists.
            val user = repo.getUser(userId)
            if (user == null) {
                sessionManager.logout()
                _sessionExpired.postValue(true)
                return@launch
            }

            // Load the user's assignments and turn them into calendar text.
            val assignments = db.assignmentDao().getAssignments(userId)
            val summary = CalendarSummary(
                titleText = "Calendar for ${user.name}",
                itemsText = buildCalendarItemsText(assignments)
            )

            // Send the finished calendar data back to the UI.
            _calendarSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    // Turn the saved assignments into a simple readable list for the calendar screen.
    private fun buildCalendarItemsText(assignments: List<Assignment>): String {
        if (assignments.isEmpty()) {
            return "No calendar items yet"
        }

        val datedAssignments = assignments
            .map { assignment -> assignment to parseDueDate(assignment.dueDate) }
            .sortedWith(
                compareBy<Pair<Assignment, LocalDateTime?>> { it.second == null }
                    .thenBy { it.second ?: LocalDateTime.MAX }
                    .thenBy { it.first.title.lowercase() }
            )

        return datedAssignments.joinToString("\n\n") { (assignment, dueAt) ->
            val dueText = if (dueAt == null) {
                assignment.dueDate?.takeIf { it.isNotBlank() } ?: "No due date saved"
            } else {
                formatDueDate(dueAt)
            }
            "${assignment.title}\nDue: $dueText"
        }
    }

    // Try a few simple date formats so the calendar screen can read saved assignment due dates.
    private fun parseDueDate(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) {
            return null
        }

        val trimmedValue = value.trim()

        try {
            return LocalDateTime.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
        }

        try {
            return OffsetDateTime.parse(trimmedValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
        } catch (_: Exception) {
        }

        try {
            return LocalDateTime.parse(trimmedValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (_: Exception) {
        }

        try {
            return LocalDate.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
        } catch (_: Exception) {
        }

        return null
    }

    // Format the due date in a simple readable way for the calendar screen.
    private fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }
}


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
import uws.ac.uk.studymate.util.SessionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

// Holds one assignment that should appear inside a calendar day cell.
data class CalendarAssignmentEntry(
    val subjectName: String,
    val assignmentTitle: String,
    val dueAt: LocalDateTime,
    val subjectColorHex: String?
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

    // Use the session manager so this screen always reads data for the logged-in user.
    private val sessionManager = SessionManager(application)

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

            // Load the assignments and the subject colors that the calendar needs.
            val assignments = db.assignmentDao().getAssignments(userId)
            val subjectsById = db.subjectDao().getSubjects(userId).associateBy { it.id }

            val entriesByDate = assignments
                .mapNotNull { assignment ->
                    val dueAt = parseDueDate(assignment.dueDate) ?: return@mapNotNull null
                    val subject = subjectsById[assignment.subjectId]
                    CalendarAssignmentEntry(
                        subjectName = subject?.name ?: "Unknown subject",
                        assignmentTitle = assignment.title,
                        dueAt = dueAt,
                        subjectColorHex = subject?.color
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
}

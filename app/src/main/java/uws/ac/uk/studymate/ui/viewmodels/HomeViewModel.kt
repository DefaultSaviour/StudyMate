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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
/*//////////////////////
Coded by Jamie Coleman
15/03/26 - ??? was it ???
updated 18/03/26
updated 28/03/26
updated 09/04/26
 *//////////////////////
// Holds the small set of values that the home screen needs to display.
data class HomeSummary(
    val welcomeText: String,
    val nextDueCountdown: String,
    val nextDueDetails: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's data.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the session manager so this screen always reads data for the logged-in user.
    private val sessionManager = SessionManager(application)

    // This private value stores the latest home screen summary.
    // It is mutable here so only the ViewModel can change it.
    private val _homeSummary = MutableLiveData<HomeSummary>()

    // This public version lets the UI observe the latest home screen data.
    val homeSummary: LiveData<HomeSummary> = _homeSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    // Disabled for now: this testing-only ClearAllData state was used to tell the UI
    // that every table had been wiped. It is commented out so it can be re-enabled later.
//    private val _allDataCleared = MutableLiveData<Boolean>()
//
//    // Disabled for now: this testing-only observer value was used by the home screen
//    // after wiping the database. It is commented out so it can be re-enabled later.
//    val allDataCleared: LiveData<Boolean> = _allDataCleared

    fun loadHome() {
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

            // Load the user's assignments for the home screen.
            val assignments = db.assignmentDao().getAssignments(userId)
            val nextDueAssignment = findNextDueAssignment(assignments)

            // Build the text that the home screen still needs to show.
            val summary = HomeSummary(
                welcomeText = "Welcome back, ${user.name}",
                nextDueCountdown = buildCountdownText(nextDueAssignment?.second),
                nextDueDetails = buildNextDueDetails(nextDueAssignment)
            )

            // Send the finished dashboard data back to the UI.
            _homeSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }


    // Disabled for now: this testing-only ClearAllData helper wiped every Room table.
    // It is commented out so it stays in the file and can be re-enabled later.
//    fun clearAllData() {
//        // Run the reset work on a background thread because Room does not allow it on the UI thread.
//        viewModelScope.launch(Dispatchers.IO) {
//
//            // Remove the saved session first so no old user stays logged in after the wipe.
//            sessionManager.logout()
//
//            // Clear every Room table so the app starts again with empty data.
//            db.clearAllTables()
//
//            // Tell the UI the reset finished so it can send the user back to login.
//            _allDataCleared.postValue(true)
//        }
//    }

    // Find the next assignment with a readable due date.
    // Prefer an upcoming assignment, but fall back to the most recent overdue one when needed.
    private fun findNextDueAssignment(assignments: List<Assignment>): Pair<Assignment, LocalDateTime>? {
        val datedAssignments = assignments.mapNotNull { assignment ->
            parseDueDate(assignment.dueDate)?.let { dueAt -> assignment to dueAt }
        }

        val now = LocalDateTime.now()
        val upcoming = datedAssignments
            .filter { (_, dueAt) -> !dueAt.isBefore(now) }
            .minByOrNull { (_, dueAt) -> dueAt }

        return upcoming ?: datedAssignments
            .filter { (_, dueAt) -> dueAt.isBefore(now) }
            .maxByOrNull { (_, dueAt) -> dueAt }
    }

    // Turn the due date into a clear countdown string for the top of the dashboard.
    private fun buildCountdownText(dueAt: LocalDateTime?): String {
        if (dueAt == null) {
            return "No due assignments yet"
        }

        val now = LocalDateTime.now()
        val duration = Duration.between(now, dueAt)

        return if (duration.isNegative) {
            "Overdue by ${formatDuration(duration.abs())}"
        } else {
            "Due in ${formatDuration(duration)}"
        }
    }

    // Show the assignment name and due date under the countdown.
    private fun buildNextDueDetails(nextDueAssignment: Pair<Assignment, LocalDateTime>?): String {
        if (nextDueAssignment == null) {
            return "Add an assignment with a due date to see your next deadline here."
        }

        val assignment = nextDueAssignment.first
        val dueAt = nextDueAssignment.second
        return "${assignment.title}\n${formatDueDate(dueAt)}"
    }


    // Try a few simple date formats so the dashboard can read saved assignment due dates.
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

    // Format the due date in a simple readable way for the home screen.
    private fun formatDueDate(dueAt: LocalDateTime): String {
        return dueAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }

    // Turn a duration into short plain English.
    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.minusDays(days).toHours()
        val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
        val parts = mutableListOf<String>()

        if (days > 0) {
            parts += "$days day" + if (days == 1L) "" else "s"
        }

        if (hours > 0) {
            parts += "$hours hour" + if (hours == 1L) "" else "s"
        }

        if (days == 0L && minutes > 0) {
            parts += "$minutes minute" + if (minutes == 1L) "" else "s"
        }

        if (parts.isEmpty()) {
            return "less than a minute"
        }

        return parts.take(2).joinToString(" ")
    }
}


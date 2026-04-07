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

// Holds the text that the statistics screen needs to display.
data class StatisticsSummary(
    val titleText: String,
    val statsText: String
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's saved stats.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the session manager so this screen always reads data for the logged-in user.
    private val sessionManager = SessionManager(application)

    // This private value stores the latest statistics text.
    // It is mutable here so only the ViewModel can change it.
    private val _statisticsSummary = MutableLiveData<StatisticsSummary>()

    // This public version lets the UI observe the latest statistics data.
    val statisticsSummary: LiveData<StatisticsSummary> = _statisticsSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadStatistics() {
        // Run the database work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {

            // Stop early when there is no logged-in user saved in the session.
            val userId = sessionManager.getLoggedInUserId()
            if (userId == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            // Load the user together with their settings and stats.
            val userWithMeta = repo.getUserWithMeta(userId)
            if (userWithMeta == null) {
                sessionManager.logout()
                _sessionExpired.postValue(true)
                return@launch
            }

            // Build the statistics text for this screen.
            val assignmentsCount = db.assignmentDao().getAssignments(userId).size
            val stats = userWithMeta.stats
            val summary = StatisticsSummary(
                titleText = "Statistics for ${userWithMeta.user.name}",
                statsText = buildStatisticsText(
                    assignmentCount = assignmentsCount,
                    flashcardCount = stats?.flashcardsCount ?: 0,
                    streakDays = stats?.streakDays ?: 0
                )
            )

            // Send the finished statistics data back to the UI.
            _statisticsSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    // Turn the saved numbers into plain English for the statistics screen.
    private fun buildStatisticsText(
        assignmentCount: Int,
        flashcardCount: Int,
        streakDays: Int
    ): String {
        return "Assignments: $assignmentCount\nFlashcards: $flashcardCount\nStreak: $streakDays day(s)"
    }
}


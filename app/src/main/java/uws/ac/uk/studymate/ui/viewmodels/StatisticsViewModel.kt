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
import uws.ac.uk.studymate.util.SessionUserResolver
/*//////////////////////
Coded by Jamie Coleman
05/04/26
fixed 09/04/26
 *//////////////////////
// Holds one small metric card shown near the top of the statistics screen.
data class StatisticsMetric(
    val label: String,
    val value: String
)

// Holds one subject row shown in the progress list underneath the summary cards.
data class SubjectDeckProgress(
    val subjectName: String,
    val deckLabel: String,
    val completedCards: Int,
    val totalCards: Int,
    val colorHex: String?
)

// Holds all of the data that the statistics screen needs to display.
data class StatisticsSummary(
    val titleText: String,
    val metrics: List<StatisticsMetric>,
    val subjectProgress: List<SubjectDeckProgress>
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load the current user's saved stats.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep user lookup logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the shared session resolver so login validation stays consistent with other screens.
    private val sessionResolver = SessionUserResolver(application, repo)

    // This private value stores the latest statistics screen data.
    // It is mutable here so only the ViewModel can change it.
    private val _statisticsSummary = MutableLiveData<StatisticsSummary>()

    // This public version lets the UI observe the latest statistics screen data.
    val statisticsSummary: LiveData<StatisticsSummary> = _statisticsSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadStatistics() {
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

            // Load the saved records that the statistics screen needs.
            val assignments = db.assignmentDao().getAssignments(userId)
            val subjects = db.subjectDao().getSubjects(userId)
            val progressBySubject = db.subjectProgressDao().getAll(userId).associateBy { it.subjectId }
            val decksWithCards = db.deckDao().getDecksWithCards(userId)
            val decksBySubject = decksWithCards.groupBy { it.deck.subjectId }

            // Build the subject progress list shown underneath the summary cards.
            // For now this uses SubjectProgress as the best available source for
            // "completed cards" until card-level completion is added later.
            val subjectProgress = subjects
                .sortedBy { it.name.lowercase() }
                .map { subject ->
                    val subjectDecks = decksBySubject[subject.id].orEmpty()
                    val cardsMade = subjectDecks.sumOf { it.cards.size }
                    val progress = progressBySubject[subject.id]
                    val totalCards = maxOf(cardsMade, progress?.totalTasks ?: 0)
                    val completedCards = (progress?.completedTasks ?: 0).coerceIn(0, totalCards)
                    val deckLabel = when (subjectDecks.size) {
                        0 -> "No deck yet"
                        1 -> subjectDecks.first().deck.name
                        else -> "${subjectDecks.size} decks"
                    }

                    SubjectDeckProgress(
                        subjectName = subject.name,
                        deckLabel = deckLabel,
                        completedCards = completedCards,
                        totalCards = totalCards,
                        colorHex = subject.color
                    )
                }

            val totalDecks = decksWithCards.size
            val totalFlashcards = decksWithCards.sumOf { it.cards.size }
            val subjectsWithTrackedProgress = subjectProgress.count { it.totalCards > 0 }

            val summary = StatisticsSummary(
                titleText = "Statistics for ${user.name}",
                metrics = buildStatisticsMetrics(
                    assignmentsCount = assignments.size,
                    flashcardsCount = totalFlashcards,
                    subjectCount = subjects.size,
                    deckCount = totalDecks,
                    subjectsWithTrackedProgress = subjectsWithTrackedProgress
                ),
                subjectProgress = subjectProgress
            )

            // Send the finished statistics data back to the UI.
            _statisticsSummary.postValue(summary)
            _sessionExpired.postValue(false)
        }
    }

    // Build the four summary cards shown near the top of the statistics screen.
    private fun buildStatisticsMetrics(
        assignmentsCount: Int,
        flashcardsCount: Int,
        subjectCount: Int,
        deckCount: Int,
        subjectsWithTrackedProgress: Int
    ): List<StatisticsMetric> {
        return listOf(
            StatisticsMetric(label = "Assignments saved", value = assignmentsCount.toString()),
            StatisticsMetric(label = "Flashcards saved", value = flashcardsCount.toString()),
            StatisticsMetric(label = "Subjects", value = subjectCount.toString()),
            StatisticsMetric(
                label = "Decks with tracked progress",
                value = "$subjectsWithTrackedProgress/$deckCount"
            )
        )
    }
}


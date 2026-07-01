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
import uws.ac.uk.studymate.util.SpacedRepetition
import uws.ac.uk.studymate.util.StreakCalculator
import uws.ac.uk.studymate.util.TrophyProgress
import java.time.LocalDate

/*//////////////////////
Trophy Room (1.2): nine trophies, each with 5 tiers (Bronze..Diamond). Every value is
computed live from tables that already exist — no new table, no migration, and only
one new DAO method total (FocusSessionDao.countAll) — so tiers can never drift from
what Statistics itself reports:
  - architect/sprinter/unbroken: Flash_Cards count, Focus_Sessions count, and the
    same streak calculation Statistics uses (StreakCalculator).
  - scholar: Assignments where AssignmentDateTimeUtils.isComplete (same rule as
    everywhere else in the app).
  - reviewer: size of the same review-timestamp list already fetched for the streak.
  - marathoner: lifetime focused hours, via sumFocusedSecondsSince from the epoch
    (reuses the existing time-windowed query rather than adding a new one).
  - collector: deck count.
  - organizer: sum of "done" across AssignmentTaskDao.progressForUser (the same
    per-assignment counts the Assignments list already shows).
  - ace: FlashCardDao.countMature (same "mature" definition Statistics uses).
"Cards authored", "streak", and "assignments completed" are live snapshots (can go
down if cards are deleted, a streak breaks, or an assignment is un-completed) rather
than permanently-locked lifetime counters — see plan.md's "Open decisions".
 *//////////////////////
data class TrophyUiState(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val value: Int,
    val tier: TrophyProgress.Tier,
    val nextThreshold: Int?      // null once at Diamond
)

class TrophyRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _trophies = MutableLiveData<List<TrophyUiState>>()
    val trophies: LiveData<List<TrophyUiState>> = _trophies

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadTrophies() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            val userId = session.userId

            val cardsAuthored = db.cardDao().countAll(userId)
            val focusSessions = db.focusSessionDao().countAll(userId)
            val reviewTimestamps = db.reviewLogDao().getReviewTimestamps(userId)
            val streak = StreakCalculator.compute(reviewTimestamps, today = LocalDate.now())
            val totalReviews = reviewTimestamps.size

            val assignments = db.assignmentDao().getAssignments(userId)
            val assignmentsCompleted = assignments.count {
                AssignmentDateTimeUtils.isComplete(it.completedAt, it.dueDate)
            }

            val decksCreated = db.deckDao().getDecks(userId).size

            val checklistDone = db.assignmentTaskDao().progressForUser(userId).sumOf { it.done }

            val matureCards = db.cardDao().countMature(userId, SpacedRepetition.MATURE_INTERVAL_DAYS)

            // Lifetime total — reuses the existing time-windowed query with an
            // epoch cutoff rather than adding a dedicated "sum all" query.
            val totalFocusedSeconds = db.focusSessionDao().sumFocusedSecondsSince(userId, "1970-01-01T00:00:00Z")
            val focusedHours = totalFocusedSeconds / 3600

            val values = mapOf(
                "architect" to cardsAuthored,
                "sprinter" to focusSessions,
                "unbroken" to streak,
                "scholar" to assignmentsCompleted,
                "reviewer" to totalReviews,
                "marathoner" to focusedHours,
                "collector" to decksCreated,
                "organizer" to checklistDone,
                "ace" to matureCards
            )

            val states = TrophyProgress.ROSTER.map { def ->
                val value = values[def.id] ?: 0
                TrophyUiState(
                    id = def.id,
                    name = def.name,
                    description = def.description,
                    iconRes = def.iconRes,
                    value = value,
                    tier = TrophyProgress.tierFor(value, def.thresholds),
                    nextThreshold = TrophyProgress.progressToNext(value, def.thresholds)?.second
                )
            }
            _trophies.postValue(states)
            _sessionExpired.postValue(false)
        }
    }
}

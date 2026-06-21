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
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.SpacedRepetition
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
/*//////////////////////
Computes the statistics dashboard live from the database (User_Stats is not used
— it was never kept up to date). Mirrors the "AT A GLANCE" approach in
UserSettingsViewModel: resolve the user, then count.
 *//////////////////////
data class StatsSummary(
    val cardsDue: Int,
    val reviewedToday: Int,
    val reviewedThisWeek: Int,
    val streakDays: Int,
    val matureCards: Int,
    val totalCards: Int,
    val assignmentsCompleted: Int,
    val assignmentsCompletedThisWeek: Int,
    val assignmentsPending: Int,
    val assignmentsDueThisWeek: Int
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _summary = MutableLiveData<StatsSummary>()
    val summary: LiveData<StatsSummary> = _summary

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            val userId = session.userId
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val todayStr = today.toString()
            val startOfTodayIso = today.atStartOfDay(zone).toInstant().toString()
            val weekAgoIso = Instant.now().minus(Duration.ofDays(7)).toString()

            // ── Flashcards ──
            val cardsDue = db.cardDao().countDue(userId, todayStr)
            val totalCards = db.cardDao().countAll(userId)
            val matureCards = db.cardDao().countMature(userId, SpacedRepetition.MATURE_INTERVAL_DAYS)
            val reviewedToday = db.reviewLogDao().countReviewsSince(userId, startOfTodayIso)
            val reviewedThisWeek = db.reviewLogDao().countReviewsSince(userId, weekAgoIso)
            val streak = computeStreak(db.reviewLogDao().getReviewTimestamps(userId), zone, today)

            // ── Assignments ──
            val assignments = db.assignmentDao().getAssignments(userId)
            val now = LocalDateTime.now()
            val weekFromNow = now.plusDays(7)
            val weekAgo = now.minusDays(7)
            val weekAgoInstant = Instant.now().minus(Duration.ofDays(7))

            // An assignment counts as complete if it was marked done OR its due
            // date has passed (a passed deadline is treated as done, not overdue).
            fun dueOf(a: Assignment) = AssignmentDateTimeUtils.parseDueDate(a.dueDate)
            fun isComplete(a: Assignment) =
                AssignmentDateTimeUtils.isComplete(a.completedAt, a.dueDate, now)

            val completedList = assignments.filter { isComplete(it) }
            val pendingList = assignments.filter { !isComplete(it) }
            val completedThisWeek = completedList.count { a ->
                if (a.completedAt != null) {
                    val at = runCatching { Instant.parse(a.completedAt) }.getOrNull()
                    at != null && at.isAfter(weekAgoInstant)
                } else {
                    val due = dueOf(a)
                    due != null && !due.isBefore(weekAgo) && due.isBefore(now)
                }
            }
            val dueThisWeek = pendingList.count { a ->
                val due = dueOf(a) ?: return@count false
                !due.isBefore(now) && due.isBefore(weekFromNow)
            }

            _summary.postValue(
                StatsSummary(
                    cardsDue = cardsDue,
                    reviewedToday = reviewedToday,
                    reviewedThisWeek = reviewedThisWeek,
                    streakDays = streak,
                    matureCards = matureCards,
                    totalCards = totalCards,
                    assignmentsCompleted = completedList.size,
                    assignmentsCompletedThisWeek = completedThisWeek,
                    assignmentsPending = pendingList.size,
                    assignmentsDueThisWeek = dueThisWeek
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    // Consecutive days (ending today, or yesterday if today isn't studied yet)
    // on which at least one review happened.
    private fun computeStreak(timestamps: List<String>, zone: ZoneId, today: LocalDate): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps
            .mapNotNull { runCatching { Instant.parse(it).atZone(zone).toLocalDate() }.getOrNull() }
            .toHashSet()
        var cursor = today
        if (!days.contains(cursor)) cursor = cursor.minusDays(1)
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

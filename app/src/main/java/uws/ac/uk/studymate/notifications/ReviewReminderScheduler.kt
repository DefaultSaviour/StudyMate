package uws.ac.uk.studymate.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import uws.ac.uk.studymate.data.entities.User
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
/*//////////////////////
Schedules a single "your flashcards are due" reminder per user, fired on the day
the SM-2 schedule next brings cards due (at 09:00 local). Re-scheduled whenever a
review session opens/finishes, so it always tracks the current soonest due date.

One job per user, unique name "review_reminder_user_<id>", tag "review_user_<id>"
(ExistingWorkPolicy.REPLACE) so we never stack stale fires.
 *//////////////////////
object ReviewReminderScheduler {

    fun scheduleNextReview(context: Context, user: User, nextDueDate: String?) {
        cancelForUser(context, user.id)
        if (user.pushNotificationsEnabled != true) return
        if (nextDueDate.isNullOrBlank()) return

        val fireAt = try {
            LocalDate.parse(nextDueDate).atTime(9, 0)
        } catch (_: Exception) {
            return
        }
        val now = LocalDateTime.now()
        if (!fireAt.isAfter(now)) return

        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(Duration.between(now, fireAt).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putInt(ReviewReminderWorker.KEY_USER_ID, user.id).build())
            .addTag(tagForUser(user.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueNameFor(user.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelForUser(context: Context, userId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagForUser(userId))
    }

    private fun tagForUser(id: Int) = "review_user_$id"
    private fun uniqueNameFor(userId: Int) = "review_reminder_user_$userId"
}

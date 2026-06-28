package uws.ac.uk.studymate.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REVIEW
            putExtra(ReviewReminderNotifier.KEY_USER_ID, user.id)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            requestCodeFor(user.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    suspend fun scheduleForUser(context: Context, userId: Int) {
        val db = StudyMateDatabase.getInstance(context)
        val user = db.userDao().getById(userId) ?: return
        val nextDue = db.cardDao().getNextDueDateActive(userId, LocalDate.now().toString(), LocalDateTime.now().toString())
        scheduleNextReview(context, user, nextDue)
    }

    fun cancelForUser(context: Context, userId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REVIEW
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCodeFor(userId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun requestCodeFor(userId: Int) = 100_000 + userId
}

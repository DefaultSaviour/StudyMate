package uws.ac.uk.studymate.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import uws.ac.uk.studymate.data.entities.CustomEvent
import uws.ac.uk.studymate.data.entities.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object CustomEventScheduler {

    fun scheduleForEvent(context: Context, event: CustomEvent, user: User) {
        cancelForEvent(context, event.id)

        if (user.pushNotificationsEnabled != true) return
        if (!event.remindDayBefore) return

        val date = try {
            LocalDate.parse(event.date)
        } catch (e: Exception) {
            return
        }

        // Fire at 09:00 AM the day before
        val fireAt = date.minusDays(1).atTime(9, 0)
        val now = LocalDateTime.now()
        
        if (!fireAt.isAfter(now)) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CUSTOM_EVENT
            putExtra(CustomEventNotifier.KEY_EVENT_ID, event.id)
            putExtra(CustomEventNotifier.KEY_USER_ID, user.id)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            requestCodeFor(event.id),
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

    fun cancelForEvent(context: Context, eventId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CUSTOM_EVENT
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCodeFor(eventId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun requestCodeFor(eventId: Int) = eventId * 100 + 42
}

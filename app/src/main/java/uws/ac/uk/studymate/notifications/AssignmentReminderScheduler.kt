package uws.ac.uk.studymate.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import java.time.LocalDateTime
import java.time.ZoneId

object AssignmentReminderScheduler {

    fun scheduleForAssignment(context: Context, assignment: Assignment, user: User) {
        cancelForAssignment(context, assignment.id)

        if (user.pushNotificationsEnabled != true) return
        val dueDate = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return

        val now = LocalDateTime.now()
        val reminders = listOf(
            AssignmentReminderNotifier.REMINDER_WEEK  to dueDate.minusDays(7),
            AssignmentReminderNotifier.REMINDER_DAY   to dueDate.minusDays(1),
            AssignmentReminderNotifier.REMINDER_TODAY to dueDate.toLocalDate().atTime(8, 0)
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        reminders.forEach { (type, fireAt) ->
            if (!fireAt.isAfter(now)) return@forEach

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ASSIGNMENT
                putExtra(AssignmentReminderNotifier.KEY_ASSIGNMENT_ID, assignment.id)
                putExtra(AssignmentReminderNotifier.KEY_USER_ID, user.id)
                putExtra(AssignmentReminderNotifier.KEY_REMINDER_TYPE, type)
            }

            val pi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(assignment.id, type),
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
    }

    fun cancelForAssignment(context: Context, assignmentId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val types = listOf(
            AssignmentReminderNotifier.REMINDER_WEEK,
            AssignmentReminderNotifier.REMINDER_DAY,
            AssignmentReminderNotifier.REMINDER_TODAY
        )
        types.forEach { type ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ASSIGNMENT
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(assignmentId, type),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun rescheduleAllForUser(context: Context, user: User, assignments: List<Assignment>) {
        if (user.pushNotificationsEnabled != true) return
        assignments.forEach { scheduleForAssignment(context, it, user) }
    }

    suspend fun cancelAllForUser(context: Context, userId: Int) {
        val db = uws.ac.uk.studymate.data.StudyMateDatabase.getInstance(context)
        val assignments = db.assignmentDao().getAssignments(userId)
        assignments.forEach { cancelForAssignment(context, it.id) }
    }

    private fun requestCodeFor(assignmentId: Int, type: String) =
        assignmentId * 10 + type.hashCode().and(0x3)
}

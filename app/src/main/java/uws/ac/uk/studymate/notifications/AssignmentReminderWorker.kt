package uws.ac.uk.studymate.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.StudyMateApplication
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.ui.LoginActivity

/*//////////////////////
Fires one assignment-reminder notification.

Inputs (all required):
  assignmentId : Int
  userId       : Int
  reminderType : "week" | "day" | "today"

At fire time the worker re-checks state so we don't surface stale reminders:
  - assignment still exists
  - the owning user still exists and still has push notifications enabled

If any of those fail, the worker quietly returns success — there's nothing to
re-schedule, the system just drops this one fire.
 *//////////////////////
class AssignmentReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val assignmentId = inputData.getInt(KEY_ASSIGNMENT_ID, -1)
        val userId       = inputData.getInt(KEY_USER_ID, -1)
        val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: return@withContext Result.success()
        if (assignmentId <= 0 || userId <= 0) return@withContext Result.success()

        val db = StudyMateDatabase.getInstance(applicationContext)
        val assignment = db.assignmentDao().getById(assignmentId) ?: return@withContext Result.success()
        if (assignment.userId != userId) return@withContext Result.success()

        val user = db.userDao().getById(userId) ?: return@withContext Result.success()
        if (user.pushNotificationsEnabled != true) return@withContext Result.success()

        postNotification(user.name, assignment.title, reminderType, assignmentId)
        Result.success()
    }

    private fun postNotification(
        userName: String,
        assignmentTitle: String,
        reminderType: String,
        assignmentId: Int
    ) {
        // Bail out silently if the user revoked POST_NOTIFICATIONS — better
        // than crashing the worker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val whenText = when (reminderType) {
            REMINDER_WEEK  -> "is due in a week"
            REMINDER_DAY   -> "is due tomorrow"
            REMINDER_TODAY -> "is due today"
            else           -> "is due soon"
        }
        val body = "$userName: ‘$assignmentTitle’ $whenText."

        val tapIntent = Intent(applicationContext, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(LoginActivity.EXTRA_NOTIFICATION_USERNAME, userName)
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            assignmentId * 10 + reminderType.hashCode().and(0x3),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(
            applicationContext,
            StudyMateApplication.CHANNEL_ASSIGNMENT_REMINDERS
        )
            .setSmallIcon(R.drawable.ic_studymate_logo)
            .setContentTitle("Assignment reminder")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Use a stable per-assignment + per-reminder id so re-fires replace
        // rather than stack.
        val notifId = assignmentId * 10 + when (reminderType) {
            REMINDER_WEEK  -> 1
            REMINDER_DAY   -> 2
            REMINDER_TODAY -> 3
            else           -> 0
        }
        try {
            NotificationManagerCompat.from(applicationContext).notify(notifId, notif)
        } catch (_: SecurityException) {
            // Permission was just revoked between the check above and the call.
        }
    }

    companion object {
        const val KEY_ASSIGNMENT_ID  = "assignmentId"
        const val KEY_USER_ID        = "userId"
        const val KEY_REMINDER_TYPE  = "reminderType"

        const val REMINDER_WEEK  = "week"
        const val REMINDER_DAY   = "day"
        const val REMINDER_TODAY = "today"
    }
}

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.StudyMateApplication
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.ui.LoginActivity

object AssignmentReminderNotifier {

    suspend fun fire(context: Context, intent: Intent) = withContext(Dispatchers.IO) {
        val assignmentId = intent.getIntExtra(KEY_ASSIGNMENT_ID, -1)
        val userId       = intent.getIntExtra(KEY_USER_ID, -1)
        val reminderType = intent.getStringExtra(KEY_REMINDER_TYPE) ?: return@withContext
        if (assignmentId <= 0 || userId <= 0) return@withContext

        val db = StudyMateDatabase.getInstance(context)
        val assignment = db.assignmentDao().getById(assignmentId) ?: return@withContext
        if (assignment.userId != userId) return@withContext

        val user = db.userDao().getById(userId) ?: return@withContext
        if (user.pushNotificationsEnabled != true) return@withContext

        postNotification(context, user.name, assignment.title, reminderType, assignmentId)
    }

    private fun postNotification(
        context: Context,
        userName: String,
        assignmentTitle: String,
        reminderType: String,
        assignmentId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
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

        val tapIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(LoginActivity.EXTRA_NOTIFICATION_USERNAME, userName)
        }
        val pi = PendingIntent.getActivity(
            context,
            assignmentId * 10 + reminderType.hashCode().and(0x3),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(
            context,
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

        val notifId = assignmentId * 10 + when (reminderType) {
            REMINDER_WEEK  -> 1
            REMINDER_DAY   -> 2
            REMINDER_TODAY -> 3
            else           -> 0
        }
        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) {}
    }

    const val KEY_ASSIGNMENT_ID  = "assignmentId"
    const val KEY_USER_ID        = "userId"
    const val KEY_REMINDER_TYPE  = "reminderType"

    const val REMINDER_WEEK  = "week"
    const val REMINDER_DAY   = "day"
    const val REMINDER_TODAY = "today"
}

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

object CustomEventNotifier {

    suspend fun fire(context: Context, intent: Intent) = withContext(Dispatchers.IO) {
        val eventId = intent.getIntExtra(KEY_EVENT_ID, -1)
        val userId  = intent.getIntExtra(KEY_USER_ID, -1)
        if (eventId <= 0 || userId <= 0) return@withContext

        val db = StudyMateDatabase.getInstance(context)
        val event = db.customEventDao().getById(eventId) ?: return@withContext
        if (event.userId != userId) return@withContext
        if (!event.remindDayBefore) return@withContext

        val user = db.userDao().getById(userId) ?: return@withContext
        if (user.pushNotificationsEnabled != true) return@withContext

        postNotification(context, user.name, event.title, eventId)
    }

    private fun postNotification(
        context: Context,
        userName: String,
        eventTitle: String,
        eventId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val body = "$userName: ‘$eventTitle’ is happening tomorrow."

        val tapIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(LoginActivity.EXTRA_NOTIFICATION_USERNAME, userName)
        }
        val pi = PendingIntent.getActivity(
            context,
            eventId * 100 + 42,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(
            context,
            StudyMateApplication.CHANNEL_CUSTOM_EVENTS
        )
            .setSmallIcon(R.drawable.ic_studymate_logo)
            .setContentTitle("Event reminder")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notifId = eventId * 100 + 42
        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) {}
    }

    const val KEY_EVENT_ID = "eventId"
    const val KEY_USER_ID  = "userId"
}

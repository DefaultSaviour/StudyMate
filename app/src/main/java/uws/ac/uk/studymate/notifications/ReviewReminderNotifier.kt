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
import java.time.LocalDate
import java.time.LocalDateTime

object ReviewReminderNotifier {

    suspend fun fire(context: Context, intent: Intent) = withContext(Dispatchers.IO) {
        val userId = intent.getIntExtra(KEY_USER_ID, -1)
        if (userId <= 0) return@withContext

        val db = StudyMateDatabase.getInstance(context)
        val user = db.userDao().getById(userId) ?: return@withContext
        if (user.pushNotificationsEnabled != true) return@withContext

        val dueCount = db.cardDao().countDueActive(
            userId, LocalDate.now().toString(), LocalDateTime.now().toString()
        )
        if (dueCount <= 0) return@withContext

        postNotification(context, user.name, dueCount, userId)
    }

    private fun postNotification(context: Context, userName: String, dueCount: Int, userId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val cards = if (dueCount == 1) "card" else "cards"
        val body = "$userName: you have $dueCount $cards ready to review."

        val tapIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(LoginActivity.EXTRA_NOTIFICATION_USERNAME, userName)
        }
        val pi = PendingIntent.getActivity(
            context,
            100_000 + userId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(
            context,
            StudyMateApplication.CHANNEL_REVIEW_REMINDERS
        )
            .setSmallIcon(R.drawable.ic_studymate_logo)
            .setContentTitle("Time to review")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(900_000 + userId, notif)
        } catch (_: SecurityException) {}
    }

    const val KEY_USER_ID = "userId"
}

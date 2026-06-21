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
import uws.ac.uk.studymate.R
import uws.ac.uk.studymate.StudyMateApplication
import uws.ac.uk.studymate.ui.FocusTimerActivity

/*//////////////////////
Fires the focus-timer phase-complete notification (0.9G).

This is the *background fallback* only: while the timer screen is alive the
ViewModel chimes/notifies at the exact boundary itself. WorkManager fires this
job if the app was minimised when a phase ended (timing is best-effort — Doze can
defer it; the precise always-on path is a deferred foreground service).

Inputs:
  message : String — the body to show ("Focus block done — time for a break", …)
Tapping the notification reopens the focus timer (it needs no session/DB).
 *//////////////////////
class FocusTimerWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val message = inputData.getString(KEY_MESSAGE) ?: return Result.success()
        postNotification(message)
        return Result.success()
    }

    private fun postNotification(message: String) {
        // Bail out silently if the user never granted / revoked POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(applicationContext, FocusTimerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            NOTIF_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(
            applicationContext,
            StudyMateApplication.CHANNEL_FOCUS_TIMER
        )
            .setSmallIcon(R.drawable.ic_studymate_logo)
            .setContentTitle("Focus timer")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID, notif)
        } catch (_: SecurityException) {
            // Permission revoked between the check above and the call.
        }
    }

    companion object {
        const val KEY_MESSAGE = "message"

        // Singular timer — one stable id so re-fires replace rather than stack.
        const val NOTIF_ID = 950_000
    }
}

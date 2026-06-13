package uws.ac.uk.studymate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

/*//////////////////////
Application entry point — currently just sets up the notification channel
that AssignmentReminderWorker fires into. Registered in the manifest as
android:name=".StudyMateApplication".
 *//////////////////////
class StudyMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createAssignmentReminderChannel()
    }

    private fun createAssignmentReminderChannel() {
        val channel = NotificationChannel(
            CHANNEL_ASSIGNMENT_REMINDERS,
            "Assignment reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Heads-up notifications for assignments approaching their due date."
            enableLights(true)
            enableVibration(true)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ASSIGNMENT_REMINDERS = "assignment_reminders"
    }
}

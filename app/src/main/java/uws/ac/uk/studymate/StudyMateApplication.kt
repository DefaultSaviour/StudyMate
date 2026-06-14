package uws.ac.uk.studymate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

/*//////////////////////
Application entry point — sets up the notification channels the WorkManager
reminders fire into. Registered in the manifest as android:name=".StudyMateApplication".
 *//////////////////////
class StudyMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createAssignmentReminderChannel()
        createReviewReminderChannel()
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
        notificationManager().createNotificationChannel(channel)
    }

    private fun createReviewReminderChannel() {
        val channel = NotificationChannel(
            CHANNEL_REVIEW_REMINDERS,
            "Flashcard reviews",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to review flashcards when they're next due (spaced repetition)."
            enableLights(true)
            enableVibration(true)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager() =
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ASSIGNMENT_REMINDERS = "assignment_reminders"
        const val CHANNEL_REVIEW_REMINDERS = "review_reminders"
    }
}

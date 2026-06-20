package uws.ac.uk.studymate

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import uws.ac.uk.studymate.util.Keyboard

/*//////////////////////
Application entry point — sets up the notification channels the WorkManager
reminders fire into. Registered in the manifest as android:name=".StudyMateApplication".

Also dismisses the soft keyboard whenever an activity is paused, so the IME never
lingers over the next screen when the user navigates away mid-typing (a quirk of
API 35 edge-to-edge — see util/Keyboard).
 *//////////////////////
class StudyMateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createAssignmentReminderChannel()
        createReviewReminderChannel()
        registerKeyboardDismissOnPause()
    }

    private fun registerKeyboardDismissOnPause() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) = Keyboard.hide(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
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

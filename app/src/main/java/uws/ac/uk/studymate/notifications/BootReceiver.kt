package uws.ac.uk.studymate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = StudyMateDatabase.getInstance(context)
                    val users = db.userDao().getAll()
                    for (user in users) {
                        if (user.pushNotificationsEnabled == true) {
                            val assignments = db.assignmentDao().getAssignments(user.id)
                            AssignmentReminderScheduler.rescheduleAllForUser(context, user, assignments)
                            ReviewReminderScheduler.scheduleForUser(context, user.id)
                            val customEvents = db.customEventDao().getEventsForUser(user.id)
                            customEvents.forEach { event ->
                                CustomEventScheduler.scheduleForEvent(context, event, user)
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

package uws.ac.uk.studymate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_ASSIGNMENT -> AssignmentReminderNotifier.fire(context, intent)
                    ACTION_FOCUS -> FocusTimerNotifier.fire(context, intent)
                    ACTION_REVIEW -> ReviewReminderNotifier.fire(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ASSIGNMENT = "uws.ac.uk.studymate.ASSIGNMENT_REMINDER"
        const val ACTION_FOCUS = "uws.ac.uk.studymate.FOCUS_TIMER"
        const val ACTION_REVIEW = "uws.ac.uk.studymate.REVIEW_REMINDER"
    }
}

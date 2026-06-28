package uws.ac.uk.studymate.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/*//////////////////////
Schedules / cancels the focus-timer phase-complete notification (0.9G).

Background fallback only — one unique one-shot at a time (the timer is singular).
The ViewModel calls [schedule] on Start with the delay to the current phase's end
and the message to show, and [cancel] on Pause / Reset / Skip. REPLACE keeps a
re-schedule from accumulating stale fires.
 *//////////////////////
object FocusTimerScheduler {

    private const val UNIQUE_NAME = "focus_timer_phase_end"

    fun schedule(context: Context, fireDelayMillis: Long, message: String) {
        // A non-positive delay would fire effectively immediately — there's
        // nothing useful to queue (the phase already ended).
        if (fireDelayMillis <= 0L) {
            cancel(context)
            return
        }

        val data = Data.Builder()
            .putString(FocusTimerWorker.KEY_MESSAGE, message)
            .build()

        val request = OneTimeWorkRequestBuilder<FocusTimerWorker>()
            .setInitialDelay(fireDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}

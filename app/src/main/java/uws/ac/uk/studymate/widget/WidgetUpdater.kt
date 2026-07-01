package uws.ac.uk.studymate.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Best-effort home-screen widget refresh, called from ~20 ViewModel mutation sites
 * (card/deck/assignment/event changes, login/logout, backup import, ...).
 *
 * Two properties that matter given how widely this is called:
 *  - Never throws into the caller. It always runs off the caller's thread and inside
 *    a try/catch, so a widget-refresh failure (including "not mocked" Android SDK
 *    calls in a plain JVM unit test with a mocked Context) can never propagate back
 *    and skip a caller's own state update (e.g. a LiveData postValue the UI is
 *    waiting on).
 *  - Debounced. A burst of calls in quick succession (e.g. grading many flashcards
 *    back-to-back in a chained review session) collapses into ONE broadcast shortly
 *    after the burst settles, instead of one broadcast + one full widget DB requery
 *    per call.
 */
object WidgetUpdater {

    // Long-lived by design: an object-level scope for a fire-and-forget mechanism
    // called from all over the app, not tied to any single screen's lifecycle.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // updateAllWidgets is called from many ViewModels on different dispatchers
    // (Main, IO, and OnboardingViewModel's own detached scope) — @Volatile so a
    // cancel/reassign on one thread is visible to a call arriving on another.
    @Volatile
    private var pendingJob: Job? = null
    private const val DEBOUNCE_MS = 400L

    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext ?: context
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(DEBOUNCE_MS)
            sendUpdateBroadcast(appContext)
        }
    }

    private fun sendUpdateBroadcast(context: Context) {
        try {
            val intent = Intent(context, TallCalendarWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, TallCalendarWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

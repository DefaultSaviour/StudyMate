package uws.ac.uk.studymate.widget

import android.content.Context
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/*//////////////////////
Regression coverage for the widget-refresh mechanism (1.1). Before this fix,
updateAllWidgets ran its Intent/AppWidgetManager work synchronously and
unguarded on the CALLER's own coroutine — which threw uncaught whenever the
Context wasn't a real Android Context (e.g. the mocked Application every
ViewModel unit test uses), breaking ReviewDeckViewModelTest, and in
production risked silently skipping a critical postValue() that was written
right after the call (deleteAccount / login / register). These tests lock
in the two properties that fix both: the call must return immediately
(never block/suspend the caller), and it must never let an exception
surface, even when the Context can't actually complete a broadcast.
 *//////////////////////
class WidgetUpdaterTest {

    private val previousHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()
    private val caught = AtomicReference<Throwable?>(null)

    @Before
    fun setup() {
        caught.set(null)
        Thread.setDefaultUncaughtExceptionHandler { _, e -> caught.set(e) }
    }

    @After
    fun teardown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    @Test
    fun `updateAllWidgets returns immediately without blocking on the debounced work`() {
        val context = mockk<Context>(relaxed = true)

        val start = System.currentTimeMillis()
        // A rapid-fire burst (e.g. grading many flashcards back-to-back) also
        // exercises the debounce's cancel-and-reschedule path.
        repeat(20) { WidgetUpdater.updateAllWidgets(context) }
        val elapsedMs = System.currentTimeMillis() - start

        // The debounce delay alone is 400ms; returning in well under that proves
        // the call is fire-and-forget rather than suspending the caller — this is
        // what stops a widget-refresh failure from ever blocking a caller's own
        // postValue().
        assertTrue("expected updateAllWidgets to return near-instantly, took ${elapsedMs}ms", elapsedMs < 200)
    }

    @Test
    fun `a context that cannot complete a broadcast never surfaces an uncaught exception`() {
        val context = mockk<Context>(relaxed = true)

        WidgetUpdater.updateAllWidgets(context)

        // The actual broadcast work is debounced onto a real background dispatcher
        // (not the JVM test's virtual clock), so give it time to genuinely run.
        Thread.sleep(700)

        assertNull(
            "WidgetUpdater let an exception escape uncaught: ${caught.get()}",
            caught.get()
        )
    }
}

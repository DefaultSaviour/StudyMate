package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/*//////////////////////
Unit tests for the SM-2 scheduler. Pure logic, runs on the JVM (no device).
 *//////////////////////
class SpacedRepetitionTest {

    private val today = LocalDate.of(2026, 6, 13)
    private fun newCard() = SpacedRepetition.State(SpacedRepetition.STARTING_EASE, 0, 0)

    // SRTEST1 — a brand-new card answered Good moves to interval 1, rep 1, ease unchanged.
    @Test
    fun newCard_good_firstIntervalIsOneDay() {
        val r = SpacedRepetition.schedule(newCard(), SpacedRepetition.GOOD, today)
        assertEquals(1, r.intervalDays)
        assertEquals(1, r.repetitions)
        assertEquals(2.5, r.easeFactor, 0.0001)
        assertEquals(today.plusDays(1), r.dueDate)
    }

    // SRTEST2 — the second successful review jumps to the fixed 6-day interval.
    @Test
    fun secondReview_good_intervalIsSixDays() {
        val afterFirst = SpacedRepetition.State(2.5, 1, 1)
        val r = SpacedRepetition.schedule(afterFirst, SpacedRepetition.GOOD, today)
        assertEquals(6, r.intervalDays)
        assertEquals(2, r.repetitions)
        assertEquals(today.plusDays(6), r.dueDate)
    }

    // SRTEST3 — from rep 2 onward the interval is round(previous * ease).
    @Test
    fun thirdReview_good_intervalIsPreviousTimesEase() {
        val state = SpacedRepetition.State(2.5, 6, 2)
        val r = SpacedRepetition.schedule(state, SpacedRepetition.GOOD, today)
        assertEquals(15, r.intervalDays) // round(6 * 2.5)
        assertEquals(3, r.repetitions)
    }

    // SRTEST4 — Again resets repetitions and interval, and lowers the ease.
    @Test
    fun again_resetsScheduleAndLowersEase() {
        val mature = SpacedRepetition.State(2.5, 40, 5)
        val r = SpacedRepetition.schedule(mature, SpacedRepetition.AGAIN, today)
        assertEquals(0, r.repetitions)
        assertEquals(1, r.intervalDays)
        assertEquals(today.plusDays(1), r.dueDate)
        assertTrue("ease should drop below the starting value", r.easeFactor < 2.5)
    }

    // SRTEST5 — Easy raises the ease, Hard lowers it (both still count as correct).
    @Test
    fun easyRaisesEase_hardLowersEase() {
        val easy = SpacedRepetition.schedule(SpacedRepetition.State(2.5, 6, 2), SpacedRepetition.EASY, today)
        val hard = SpacedRepetition.schedule(SpacedRepetition.State(2.5, 6, 2), SpacedRepetition.HARD, today)
        assertEquals(2.6, easy.easeFactor, 0.0001)
        assertEquals(2.36, hard.easeFactor, 0.0001)
        assertEquals(3, easy.repetitions)
        assertEquals(3, hard.repetitions)
    }

    // SRTEST6 — ease never falls below the floor, even after repeated failures.
    @Test
    fun ease_neverDropsBelowFloor() {
        var state = SpacedRepetition.State(SpacedRepetition.MIN_EASE, 1, 0)
        repeat(5) {
            val r = SpacedRepetition.schedule(state, SpacedRepetition.AGAIN, today)
            assertTrue(r.easeFactor >= SpacedRepetition.MIN_EASE)
            state = SpacedRepetition.State(r.easeFactor, r.intervalDays, r.repetitions)
        }
    }
}

package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uws.ac.uk.studymate.util.FocusTimerEngine.Phase
import org.junit.Test

/*//////////////////////
Unit tests for the focus / Pomodoro timer state machine (0.9G).
 *//////////////////////
class FocusTimerEngineTest {

    private val classic = FocusTimerEngine.Config(focusMinutes = 25, breakMinutes = 5, rounds = 4)

    @Test
    fun initialStartsOnFocusRoundOneNotRunning() {
        val s = FocusTimerEngine.initial(classic)
        assertEquals(Phase.FOCUS, s.phase)
        assertEquals(1, s.round)
        assertEquals(25 * 60, s.remainingSeconds)
        assertFalse(s.running)
    }

    @Test
    fun phaseDurations() {
        assertEquals(25 * 60, FocusTimerEngine.phaseDurationSeconds(Phase.FOCUS, classic))
        assertEquals(5 * 60, FocusTimerEngine.phaseDurationSeconds(Phase.BREAK, classic))
        assertEquals(0, FocusTimerEngine.phaseDurationSeconds(Phase.DONE, classic))
    }

    @Test
    fun focusAdvancesToBreakSameRound() {
        val next = FocusTimerEngine.advance(FocusTimerEngine.initial(classic), classic)
        assertEquals(Phase.BREAK, next.phase)
        assertEquals(1, next.round)
        assertEquals(5 * 60, next.remainingSeconds)
        assertTrue(next.running)
    }

    @Test
    fun breakAdvancesToNextFocusRound() {
        val breakState = FocusTimerEngine.TimerState(Phase.BREAK, round = 1, remainingSeconds = 0, running = true)
        val next = FocusTimerEngine.advance(breakState, classic)
        assertEquals(Phase.FOCUS, next.phase)
        assertEquals(2, next.round)
        assertEquals(25 * 60, next.remainingSeconds)
        assertTrue(next.running)
    }

    @Test
    fun finalBreakAdvancesToDone() {
        val lastBreak = FocusTimerEngine.TimerState(Phase.BREAK, round = 4, remainingSeconds = 0, running = true)
        val next = FocusTimerEngine.advance(lastBreak, classic)
        assertEquals(Phase.DONE, next.phase)
        assertEquals(0, next.remainingSeconds)
        assertFalse(next.running)
    }

    @Test
    fun doneIsTerminal() {
        val done = FocusTimerEngine.TimerState(Phase.DONE, round = 4, remainingSeconds = 0, running = false)
        assertEquals(done, FocusTimerEngine.advance(done, classic))
    }

    @Test
    fun singleRoundConfigFocusThenBreakThenDone() {
        val cfg = FocusTimerEngine.Config(focusMinutes = 1, breakMinutes = 1, rounds = 1)
        val focus = FocusTimerEngine.initial(cfg)
        val afterFocus = FocusTimerEngine.advance(focus, cfg)
        assertEquals(Phase.BREAK, afterFocus.phase)
        assertEquals(1, afterFocus.round)
        val afterBreak = FocusTimerEngine.advance(afterFocus, cfg)
        assertEquals(Phase.DONE, afterBreak.phase)
    }

    @Test
    fun walkFullClassicSessionCountsEightPhaseChanges() {
        // 4 rounds → FOCUS/BREAK ×4 → 8 phase ends before DONE.
        var state = FocusTimerEngine.initial(classic)
        var transitions = 0
        while (state.phase != Phase.DONE && transitions < 100) {
            state = FocusTimerEngine.advance(state, classic)
            transitions++
        }
        assertEquals(Phase.DONE, state.phase)
        assertEquals(8, transitions)
    }

    @Test
    fun customDurationsRespected() {
        val cfg = FocusTimerEngine.Config(focusMinutes = 50, breakMinutes = 10, rounds = 2)
        assertEquals(50 * 60, FocusTimerEngine.initial(cfg).remainingSeconds)
        assertEquals(10 * 60, FocusTimerEngine.advance(FocusTimerEngine.initial(cfg), cfg).remainingSeconds)
    }
}

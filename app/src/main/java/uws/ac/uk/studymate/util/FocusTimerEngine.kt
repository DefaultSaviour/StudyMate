package uws.ac.uk.studymate.util

/*//////////////////////
Focus / Pomodoro timer (0.9G).
Pure-Kotlin (Android-free, unit-tested) state machine for the focus-timer screen.
It owns only the *rules* — what the next phase is and how long each phase lasts —
so it can be unit-tested without Android (mirrors util/SpacedRepetition,
util/CsvCardParser).

A session alternates FOCUS ⇄ BREAK for [Config.rounds] rounds, then ends:
  FOCUS round 1 → BREAK round 1 → FOCUS round 2 → … → FOCUS round N → BREAK round N → DONE

The ViewModel owns the wall-clock ticking (elapsedRealtime); when a phase's
countdown reaches zero it calls [advance] to get the next phase/round.
 *//////////////////////
object FocusTimerEngine {

    enum class Phase { FOCUS, BREAK, DONE }

    data class Config(
        val focusMinutes: Int,
        val breakMinutes: Int,
        val rounds: Int
    )

    data class TimerState(
        val phase: Phase,
        val round: Int,            // 1-based; which focus block we're on (or were on, in BREAK/DONE)
        val remainingSeconds: Int,
        val running: Boolean
    )

    // The state a fresh, un-started session begins in: ready on FOCUS round 1.
    fun initial(config: Config): TimerState = TimerState(
        phase = Phase.FOCUS,
        round = 1,
        remainingSeconds = phaseDurationSeconds(Phase.FOCUS, config),
        running = false
    )

    // Full length of a phase, in seconds. DONE has no duration.
    fun phaseDurationSeconds(phase: Phase, config: Config): Int = when (phase) {
        Phase.FOCUS -> config.focusMinutes * 60
        Phase.BREAK -> config.breakMinutes * 60
        Phase.DONE -> 0
    }

    // The next state once the current phase's countdown hits zero. Auto-advances
    // (keeps running) into the next phase; stops (running = false) only at DONE.
    fun advance(state: TimerState, config: Config): TimerState = when (state.phase) {
        Phase.FOCUS -> TimerState(
            phase = Phase.BREAK,
            round = state.round,
            remainingSeconds = phaseDurationSeconds(Phase.BREAK, config),
            running = true
        )
        Phase.BREAK -> {
            if (state.round < config.rounds) {
                TimerState(
                    phase = Phase.FOCUS,
                    round = state.round + 1,
                    remainingSeconds = phaseDurationSeconds(Phase.FOCUS, config),
                    running = true
                )
            } else {
                TimerState(phase = Phase.DONE, round = config.rounds, remainingSeconds = 0, running = false)
            }
        }
        Phase.DONE -> state
    }
}

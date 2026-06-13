package uws.ac.uk.studymate.util

import java.time.LocalDate
import kotlin.math.roundToInt

// SM-2 spaced-repetition scheduler (the algorithm Anki is based on). Pure Kotlin
// so it can be unit-tested without Room/Android.
//
// The four review buttons map to an SM-2 quality score (0..5):
//   Again -> 2  (a lapse: any quality < 3 forces a relearn from interval 1)
//   Hard  -> 3  (correct, but ease drops)
//   Good  -> 4  (correct, ease unchanged)
//   Easy  -> 5  (correct, ease rises)
object SpacedRepetition {

    const val AGAIN = 0
    const val HARD = 1
    const val GOOD = 2
    const val EASY = 3

    const val STARTING_EASE = 2.5
    const val MIN_EASE = 1.3

    // A card is "mature" once its interval reaches this many days (used for stats).
    const val MATURE_INTERVAL_DAYS = 21

    data class State(val easeFactor: Double, val intervalDays: Int, val repetitions: Int)
    data class Result(
        val easeFactor: Double,
        val intervalDays: Int,
        val repetitions: Int,
        val dueDate: LocalDate
    )

    private fun qualityFor(grade: Int): Int = when (grade) {
        AGAIN -> 2   // q < 3 -> lapse / reset
        HARD -> 3
        GOOD -> 4
        EASY -> 5
        else -> 4
    }

    fun schedule(state: State, grade: Int, today: LocalDate): Result {
        val q = qualityFor(grade)

        // Ease update (SM-2 formula), clamped to the floor so cards never collapse.
        val newEase = (state.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceAtLeast(MIN_EASE)

        val newReps: Int
        val newInterval: Int
        if (q < 3) {
            // Lapse — start the ladder again, see it tomorrow.
            newReps = 0
            newInterval = 1
        } else {
            newReps = state.repetitions + 1
            newInterval = when (state.repetitions) {
                0 -> 1
                1 -> 6
                else -> (state.intervalDays * newEase).roundToInt().coerceAtLeast(1)
            }
        }

        return Result(
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = newReps,
            dueDate = today.plusDays(newInterval.toLong())
        )
    }
}

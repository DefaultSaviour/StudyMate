package uws.ac.uk.studymate.util

/*//////////////////////
Tiny pure formatter (0.9J) for focus-study durations shown on the Statistics
screen. Kept Android-free so it can be unit-tested (DurationFormatTest).
 *//////////////////////
object DurationFormat {

    // Render a whole number of minutes as a compact "Xh Ym" / "Ym" / "Xh" string.
    //   0   -> "0m"
    //   45  -> "45m"
    //   60  -> "1h"
    //   90  -> "1h 30m"
    // Negative inputs are clamped to zero.
    fun hoursMinutes(totalMinutes: Int): String {
        val mins = totalMinutes.coerceAtLeast(0)
        val h = mins / 60
        val m = mins % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}

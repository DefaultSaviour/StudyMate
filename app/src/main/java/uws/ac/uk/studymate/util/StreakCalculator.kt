package uws.ac.uk.studymate.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*//////////////////////
Pure Kotlin study-streak calculation, extracted from StatisticsViewModel.computeStreak
(1.2) so both StatisticsViewModel and TrophyRoomViewModel compute the same number the
same way rather than duplicating the algorithm. Unit-tested.
 *//////////////////////
object StreakCalculator {

    // Consecutive days (ending today, or yesterday if today isn't studied yet) on
    // which at least one review happened. [timestamps] are ISO instants.
    fun compute(timestamps: List<String>, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now()): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps
            .mapNotNull { runCatching { Instant.parse(it).atZone(zone).toLocalDate() }.getOrNull() }
            .toHashSet()
        var cursor = today
        if (!days.contains(cursor)) cursor = cursor.minusDays(1)
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

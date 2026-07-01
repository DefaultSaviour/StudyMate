package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/*//////////////////////
Regression coverage for the streak algorithm extracted from
StatisticsViewModel.computeStreak (1.2) — locks in its existing behaviour now that
TrophyRoomViewModel also depends on it.
 *//////////////////////
class StreakCalculatorTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 6, 15)

    private fun instantOn(date: LocalDate) = date.atStartOfDay(zone).toInstant().toString()

    @Test
    fun emptyTimestampsReturnZero() {
        assertEquals(0, StreakCalculator.compute(emptyList(), zone, today))
    }

    @Test
    fun studiedTodayCountsTodayInclusive() {
        val timestamps = listOf(instantOn(today), instantOn(today.minusDays(1)), instantOn(today.minusDays(2)))
        assertEquals(3, StreakCalculator.compute(timestamps, zone, today))
    }

    @Test
    fun notYetStudiedTodayStillCountsYesterdaysStreak() {
        val timestamps = listOf(instantOn(today.minusDays(1)), instantOn(today.minusDays(2)))
        assertEquals(2, StreakCalculator.compute(timestamps, zone, today))
    }

    @Test
    fun gapBreaksTheStreak() {
        val timestamps = listOf(instantOn(today), instantOn(today.minusDays(3)))
        assertEquals(1, StreakCalculator.compute(timestamps, zone, today))
    }

    @Test
    fun multipleReviewsOnSameDayCountOnceTowardStreak() {
        val d = today.atStartOfDay(zone)
        val timestamps = listOf(d.toInstant().toString(), d.plusHours(3).toInstant().toString())
        assertEquals(1, StreakCalculator.compute(timestamps, zone, today))
    }
}

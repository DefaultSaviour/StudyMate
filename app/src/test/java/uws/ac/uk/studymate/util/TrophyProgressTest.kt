package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*//////////////////////
Unit tests for the Trophy Room's tier math (1.2). Pure Kotlin, no Room/Android.
 *//////////////////////
class TrophyProgressTest {

    private val thresholds = listOf(8, 16, 32, 64, 128)

    @Test
    fun zeroValueIsNoTier() {
        assertEquals(TrophyProgress.Tier.NONE, TrophyProgress.tierFor(0, thresholds))
    }

    @Test
    fun oneBelowThresholdDoesNotCount() {
        assertEquals(TrophyProgress.Tier.NONE, TrophyProgress.tierFor(7, thresholds))
        assertEquals(TrophyProgress.Tier.BRONZE, TrophyProgress.tierFor(15, thresholds))
    }

    @Test
    fun exactlyAtThresholdCountsAsThatTier() {
        assertEquals(TrophyProgress.Tier.BRONZE, TrophyProgress.tierFor(8, thresholds))
        assertEquals(TrophyProgress.Tier.SILVER, TrophyProgress.tierFor(16, thresholds))
        assertEquals(TrophyProgress.Tier.GOLD, TrophyProgress.tierFor(32, thresholds))
        assertEquals(TrophyProgress.Tier.PLATINUM, TrophyProgress.tierFor(64, thresholds))
        assertEquals(TrophyProgress.Tier.DIAMOND, TrophyProgress.tierFor(128, thresholds))
    }

    @Test
    fun aboveDiamondStaysDiamond() {
        assertEquals(TrophyProgress.Tier.DIAMOND, TrophyProgress.tierFor(9999, thresholds))
    }

    @Test
    fun progressToNextReportsCurrentAndNextThreshold() {
        assertEquals(0 to 8, TrophyProgress.progressToNext(0, thresholds))
        assertEquals(10 to 16, TrophyProgress.progressToNext(10, thresholds))
        assertEquals(100 to 128, TrophyProgress.progressToNext(100, thresholds))
    }

    @Test
    fun progressToNextIsNullAtDiamond() {
        assertNull(TrophyProgress.progressToNext(128, thresholds))
        assertNull(TrophyProgress.progressToNext(500, thresholds))
    }

    @Test
    fun rosterHasNineTrophiesWithFiveThresholdsEach() {
        assertEquals(9, TrophyProgress.ROSTER.size)
        TrophyProgress.ROSTER.forEach { assertEquals(5, it.thresholds.size) }
    }

    @Test
    fun rosterIdsAreUnique() {
        val ids = TrophyProgress.ROSTER.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun nextTierWalksTheLadderAndCapsAtDiamond() {
        assertEquals(TrophyProgress.Tier.BRONZE, TrophyProgress.nextTier(TrophyProgress.Tier.NONE))
        assertEquals(TrophyProgress.Tier.SILVER, TrophyProgress.nextTier(TrophyProgress.Tier.BRONZE))
        assertEquals(TrophyProgress.Tier.GOLD, TrophyProgress.nextTier(TrophyProgress.Tier.SILVER))
        assertEquals(TrophyProgress.Tier.PLATINUM, TrophyProgress.nextTier(TrophyProgress.Tier.GOLD))
        assertEquals(TrophyProgress.Tier.DIAMOND, TrophyProgress.nextTier(TrophyProgress.Tier.PLATINUM))
        assertEquals(TrophyProgress.Tier.DIAMOND, TrophyProgress.nextTier(TrophyProgress.Tier.DIAMOND))
    }
}

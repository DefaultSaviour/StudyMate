package uws.ac.uk.studymate.util

import uws.ac.uk.studymate.R

/*//////////////////////
Pure Kotlin tier math for the Trophy Room (1.2). Trophies are computed live from
existing tables (card count, focus-session count, study streak) rather than stored —
see the 1.2 plan's "compute, don't store" decision. This file only knows about
numbers, thresholds, and icon resource ids (plain Ints — still JVM-unit-testable);
TrophyRoomViewModel supplies the actual counts.

ROSTER is the ONE place a trophy is defined — id, name, description, thresholds, and
icon all live on the definition, so adding a 10th trophy can't silently half-work
because a when-ladder in the Activity was forgotten.
 *//////////////////////
object TrophyProgress {

    enum class Tier { NONE, BRONZE, SILVER, GOLD, PLATINUM, DIAMOND }

    // One trophy definition: 5 ascending thresholds, Bronze through Diamond.
    data class TrophyDefinition(
        val id: String,
        val name: String,
        val description: String,
        val iconRes: Int,
        val thresholds: List<Int>
    ) {
        init {
            require(thresholds.size == 5) { "TrophyDefinition needs exactly 5 thresholds (Bronze..Diamond)" }
        }
    }

    // Owns tier ordering for the whole app — nextTier() below is derived from this,
    // so the ladder can't drift from the thresholds' Bronze..Diamond order.
    private val TIERS_ASCENDING = listOf(Tier.BRONZE, Tier.SILVER, Tier.GOLD, Tier.PLATINUM, Tier.DIAMOND)

    // The shipped trophy roster (1.2) — the original 3 named in the source proposal
    // plus 6 more, all computed the same "no new table" way from data that already
    // exists (see TrophyRoomViewModel). This list is intentionally the single place
    // to add a 10th trophy later.
    val ROSTER = listOf(
        TrophyDefinition(
            id = "architect",
            name = "The Architect",
            description = "Every flashcard you create counts toward this trophy, across every deck and assignment you own. Delete cards and the count — and the tier — can drop back down.",
            iconRes = R.drawable.ic_cards,
            thresholds = listOf(8, 16, 32, 64, 128)
        ),
        TrophyDefinition(
            id = "sprinter",
            name = "The Sprinter",
            description = "Finish focus-timer sessions to build toward this trophy. Every completed session counts, however short — pause and quit early and it won't.",
            iconRes = R.drawable.ic_bolt,
            thresholds = listOf(1, 5, 25, 50, 100)
        ),
        TrophyDefinition(
            id = "unbroken",
            name = "The Unbroken",
            description = "Review at least one flashcard on consecutive days to build a streak. Miss a whole day and the streak breaks, dropping this trophy back down.",
            iconRes = R.drawable.ic_flame,
            thresholds = listOf(3, 7, 14, 30, 100)
        ),
        TrophyDefinition(
            id = "scholar",
            name = "The Scholar",
            description = "Finish assignments — mark them done yourself, or let the due date pass — to progress this trophy. Reopening a finished assignment can lower it again.",
            iconRes = R.drawable.ic_school,
            thresholds = listOf(3, 7, 15, 30, 60)
        ),
        TrophyDefinition(
            id = "reviewer",
            name = "The Reviewer",
            description = "Every flashcard review you've ever logged — right or wrong, across every deck — counts toward this trophy. This one only ever goes up.",
            iconRes = R.drawable.ic_open_book,
            thresholds = listOf(50, 200, 500, 1500, 5000)
        ),
        TrophyDefinition(
            id = "marathoner",
            name = "The Marathoner",
            description = "The total time you've spent in completed focus sessions, added up in hours over your whole account. This one only ever goes up.",
            iconRes = R.drawable.ic_clock,
            thresholds = listOf(5, 20, 50, 100, 250)
        ),
        TrophyDefinition(
            id = "collector",
            name = "The Collector",
            description = "Create flashcard decks under your assignments to build toward this trophy. Deleting a deck lowers the count.",
            iconRes = R.drawable.ic_folders,
            thresholds = listOf(3, 6, 12, 24, 50)
        ),
        TrophyDefinition(
            id = "organizer",
            name = "The Organizer",
            description = "Tick off checklist items on your assignments to make progress here. Add a checklist from the assignment's row, then check items off as you go.",
            iconRes = R.drawable.ic_checklist,
            thresholds = listOf(5, 15, 40, 100, 250)
        ),
        TrophyDefinition(
            id = "ace",
            name = "The Ace",
            description = "Cards you've reviewed enough times to reach a long SM-2 interval count as mastered — the flashcards you truly know cold, not just recently seen.",
            iconRes = R.drawable.ic_crown,
            thresholds = listOf(5, 15, 40, 100, 250)
        )
    )

    fun tierFor(value: Int, thresholds: List<Int>): Tier {
        var tier = Tier.NONE
        for (i in thresholds.indices) {
            if (value >= thresholds[i]) tier = TIERS_ASCENDING[i]
        }
        return tier
    }

    // (current threshold reached, next threshold to reach), or null once at Diamond
    // (there's nothing further to progress toward).
    fun progressToNext(value: Int, thresholds: List<Int>): Pair<Int, Int>? {
        for (t in thresholds) {
            if (value < t) return value to t
        }
        return null
    }

    // The tier above [current] (Diamond caps at Diamond). Derived from
    // TIERS_ASCENDING so UI code never hand-encodes the ladder.
    fun nextTier(current: Tier): Tier {
        if (current == Tier.NONE) return TIERS_ASCENDING.first()
        val i = TIERS_ASCENDING.indexOf(current)
        return TIERS_ASCENDING.getOrElse(i + 1) { TIERS_ASCENDING.last() }
    }
}

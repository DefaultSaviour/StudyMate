package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase

/*//////////////////////
First-run sample-deck seeding (0.9E). Verifies SampleContentSeeder builds the
expected Getting Started → How StudyMate works tree, that cards are left due
(dueAt == null), and that seeding twice produces two independent trees (the
"every new account" semantics never dedupe).
 *//////////////////////
@RunWith(AndroidJUnit4::class)
class SampleContentSeederInstrumentedTest : RoomDbTestBase() {

    @Test
    fun seed_createsGettingStartedTreeWithDueCards() = runBlocking {
        val userId = insertUser(name = "Seeded User")

        SampleContentSeeder(db).seed(userId)

        val assignments = db.assignmentDao().getAssignments(userId)
        assertEquals("one sample assignment", 1, assignments.size)
        val assignment = assignments.first()
        assertEquals(SampleContentSeeder.ASSIGNMENT_TITLE, assignment.title)
        assertNotNull("sample assignment has a due date", assignment.dueDate)
        assertNull("sample assignment is not completed", assignment.completedAt)

        val decks = db.deckDao().getDecksForAssignment(assignment.id)
        assertEquals("one sample deck", 1, decks.size)
        val deck = decks.first()
        assertEquals(SampleContentSeeder.DECK_NAME, deck.name)
        assertEquals(userId, deck.userId)

        val cards = db.cardDao().getCards(deck.id)
        assertEquals(SampleContentSeeder.SAMPLE_CARDS.size, cards.size)
        assertTrue("all sample cards are due now (dueAt == null)", cards.all { it.dueAt == null })
        assertTrue("all sample cards belong to the user", cards.all { it.userId == userId })
        assertTrue("all sample cards belong to the deck", cards.all { it.deckId == deck.id })
    }

    @Test
    fun seed_twice_createsTwoIndependentTrees() = runBlocking {
        val userId = insertUser(name = "Repeat User")

        SampleContentSeeder(db).seed(userId)
        SampleContentSeeder(db).seed(userId)

        val assignments = db.assignmentDao().getAssignments(userId)
        assertEquals("seeding twice creates two assignments", 2, assignments.size)
        assertTrue(assignments.all { it.title == SampleContentSeeder.ASSIGNMENT_TITLE })

        val totalDecks = assignments.sumOf { db.deckDao().getDecksForAssignment(it.id).size }
        assertEquals("two decks total", 2, totalDecks)
    }
}

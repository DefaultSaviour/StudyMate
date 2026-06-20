package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class FlashCardDaoInstrumentedTest : RoomDbTestBase() {

    // CARDAO1
    // Only return cards from the chosen deck.
    // This also checks cards from a different deck are left out.
    @Test
    fun getCards_returnsCardsForThatDeck() = runBlocking {
        val userId = insertUser(email = "cards-one@example.com")
        val subjectId = insertAssignment(userId = userId, title = "French")
        val firstDeckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Basics")
        val secondDeckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Advanced")

        insertCard(userId = userId, deckId = firstDeckId, front = "Bonjour", back = "Hello")
        insertCard(userId = userId, deckId = firstDeckId, front = "Merci", back = "Thanks")
        insertCard(userId = userId, deckId = secondDeckId, front = "Au revoir", back = "Goodbye")

        val cards = db.cardDao().getCards(firstDeckId)

        assertEquals(2, cards.size)
        assertEquals(setOf("Bonjour", "Merci"), cards.map { it.front }.toSet())
    }

    // CARDAO2
    // Update one saved flash card.
    // Check the front and back text both change.
    @Test
    fun updateCard_changesSavedValues() = runBlocking {
        val userId = insertUser(email = "card-update@example.com")
        val subjectId = insertAssignment(userId = userId, title = "Spanish")
        val deckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Words")
        insertCard(userId = userId, deckId = deckId, front = "Hola", back = "Hi")

        val saved = db.cardDao().getCards(deckId).first()
        db.cardDao().update(saved.copy(front = "Adios", back = "Bye"))

        val updated = db.cardDao().getCards(deckId).first()

        assertEquals("Adios", updated.front)
        assertEquals("Bye", updated.back)
    }

    // CARDAO3
    // Delete one flash card from its deck.
    // Make sure the deck list becomes empty after the delete.
    @Test
    fun deleteCard_removesItFromTheDeck() = runBlocking {
        val userId = insertUser(email = "card-delete@example.com")
        val subjectId = insertAssignment(userId = userId, title = "German")
        val deckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Words")
        insertCard(userId = userId, deckId = deckId, front = "Ja", back = "Yes")

        val saved = db.cardDao().getCards(deckId).first()
        db.cardDao().delete(saved)

        assertEquals(0, db.cardDao().getCards(deckId).size)
    }

    // ── Active-assignment (review-eligibility) queries ──
    // An assignment is "active" when it is NOT manually completed AND its due date
    // has not passed. Cards under a completed/past-due assignment drop out of the
    // dashboard quick-review and the "cards due" reminders.

    private val today = "2026-06-20"
    private val nowIso = "2026-06-20T12:00"

    // CARDAO4
    // getDueCardsActive leaves out cards whose assignment is marked done.
    @Test
    fun getDueCardsActive_excludesCompletedAssignment() = runBlocking {
        val userId = insertUser(email = "active-done@example.com")

        val doneAssignment = insertAssignment(
            userId = userId, title = "Done",
            dueDate = "2026-12-01T09:00", completedAt = "2026-06-01T10:00"
        )
        val activeAssignment = insertAssignment(
            userId = userId, title = "Active",
            dueDate = "2026-12-01T09:00", completedAt = null
        )
        val doneDeck = insertDeck(userId, doneAssignment, "Done deck")
        val activeDeck = insertDeck(userId, activeAssignment, "Active deck")
        // due now (null due_at) in each deck
        insertCard(userId = userId, deckId = doneDeck, front = "X", dueAt = null)
        insertCard(userId = userId, deckId = activeDeck, front = "Y", dueAt = null)

        val due = db.cardDao().getDueCardsActive(userId, today, nowIso)

        assertEquals(1, due.size)
        assertEquals("Y", due.first().front)
    }

    // CARDAO5
    // A past-due assignment (not manually completed) is also treated as finished,
    // so its due cards are excluded too.
    @Test
    fun getDueCardsActive_excludesPastDueAssignment() = runBlocking {
        val userId = insertUser(email = "active-pastdue@example.com")

        val pastAssignment = insertAssignment(
            userId = userId, title = "Past", dueDate = "2026-01-01T09:00"
        )
        val futureAssignment = insertAssignment(
            userId = userId, title = "Future", dueDate = "2026-12-01T09:00"
        )
        val pastDeck = insertDeck(userId, pastAssignment, "Past deck")
        val futureDeck = insertDeck(userId, futureAssignment, "Future deck")
        insertCard(userId = userId, deckId = pastDeck, front = "Old", dueAt = null)
        insertCard(userId = userId, deckId = futureDeck, front = "New", dueAt = null)

        val due = db.cardDao().getDueCardsActive(userId, today, nowIso)

        assertEquals(1, due.size)
        assertEquals("New", due.first().front)
    }

    // CARDAO6
    // countDueActive counts only the cards from still-active assignments.
    @Test
    fun countDueActive_countsOnlyActive() = runBlocking {
        val userId = insertUser(email = "active-count@example.com")

        val doneAssignment = insertAssignment(
            userId = userId, title = "Done",
            dueDate = "2026-12-01T09:00", completedAt = "2026-06-01T10:00"
        )
        val activeAssignment = insertAssignment(
            userId = userId, title = "Active", dueDate = "2026-12-01T09:00"
        )
        val doneDeck = insertDeck(userId, doneAssignment, "Done deck")
        val activeDeck = insertDeck(userId, activeAssignment, "Active deck")
        insertCard(userId = userId, deckId = doneDeck, dueAt = null)
        insertCard(userId = userId, deckId = activeDeck, dueAt = null)
        insertCard(userId = userId, deckId = activeDeck, dueAt = today) // due today counts too

        assertEquals(2, db.cardDao().countDueActive(userId, today, nowIso))
    }

    // CARDAO7
    // getNextDueDateActive picks the soonest future due date, ignoring cards under a
    // completed assignment even when theirs is sooner.
    @Test
    fun getNextDueDateActive_ignoresCompletedAssignments() = runBlocking {
        val userId = insertUser(email = "active-next@example.com")

        val doneAssignment = insertAssignment(
            userId = userId, title = "Done",
            dueDate = "2026-12-01T09:00", completedAt = "2026-06-01T10:00"
        )
        val activeAssignment = insertAssignment(
            userId = userId, title = "Active", dueDate = "2026-12-01T09:00"
        )
        val doneDeck = insertDeck(userId, doneAssignment, "Done deck")
        val activeDeck = insertDeck(userId, activeAssignment, "Active deck")
        // The completed assignment's card comes due sooner, but should be ignored.
        insertCard(userId = userId, deckId = doneDeck, dueAt = "2026-06-25")
        insertCard(userId = userId, deckId = activeDeck, dueAt = "2026-07-01")

        assertEquals("2026-07-01", db.cardDao().getNextDueDateActive(userId, today, nowIso))
    }

    // CARDAO8
    // With every assignment finished, there is no next active due date.
    @Test
    fun getNextDueDateActive_nullWhenAllCompleted() = runBlocking {
        val userId = insertUser(email = "active-none@example.com")

        val doneAssignment = insertAssignment(
            userId = userId, title = "Done",
            dueDate = "2026-12-01T09:00", completedAt = "2026-06-01T10:00"
        )
        val doneDeck = insertDeck(userId, doneAssignment, "Done deck")
        insertCard(userId = userId, deckId = doneDeck, dueAt = "2026-07-01")

        assertNull(db.cardDao().getNextDueDateActive(userId, today, nowIso))
    }
}


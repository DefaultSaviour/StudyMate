package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
import uws.ac.uk.studymate.util.SpacedRepetition
import java.time.LocalDate
/*////////////////
Coded by Jamie Coleman
 12/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class CardRepoInstrumentedTest : RoomDbTestBase() {

    // CRDREP1
    // Save one card through the repository.
    // Check the deck list contains the saved front and back text.
    @Test
    fun addCard_savesTheCard() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-repo-save@example.com")
        val subjectId = insertAssignment(userId = userId, title = "French")
        val deckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Basics")

        repo.addCard(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = "Bonjour",
                back = "Hello"
            )
        )

        val cards = repo.getCards(deckId)

        assertEquals(1, cards.size)
        assertEquals("Bonjour", cards.first().front)
        assertEquals("Hello", cards.first().back)
    }

    // CRDREP2
    // Update one saved flash card through the repository.
    // Make sure both the front and back text are changed.
    @Test
    fun updateCard_changesSavedValues() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-repo-update@example.com")
        val subjectId = insertAssignment(userId = userId, title = "Italian")
        val deckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Basics")
        repo.addCard(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = "Ciao",
                back = "Hello"
            )
        )

        val saved = repo.getCards(deckId).first()
        repo.updateCard(saved.copy(front = "Grazie", back = "Thank you"))

        val updated = repo.getCards(deckId).first()

        assertEquals("Grazie", updated.front)
        assertEquals("Thank you", updated.back)
    }

    // CRDREP3
    // Delete one saved card through the repository.
    // Make sure the chosen deck no longer has any cards left.
    @Test
    fun deleteCard_removesTheCard() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-repo-delete@example.com")
        val subjectId = insertAssignment(userId = userId, title = "Spanish")
        val deckId = insertDeck(userId = userId, assignmentId = subjectId, name = "Words")
        repo.addCard(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = "Hola",
                back = "Hi"
            )
        )

        val saved = repo.getCards(deckId).first()
        repo.deleteCard(saved)

        assertEquals(0, repo.getCards(deckId).size)
    }

    // CRDREP4
    // Spaced repetition: only cards with no due date or a due date today/earlier
    // are returned as due; future-dated cards are excluded.
    @Test
    fun getDueCardsForDeck_returnsOnlyDueOrNewCards() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-due@example.com")
        val subjectId = insertAssignment(userId = userId)
        val deckId = insertDeck(userId = userId, assignmentId = subjectId)
        val today = LocalDate.now()

        insertCard(userId, deckId, front = "new", dueAt = null)
        insertCard(userId, deckId, front = "overdue", dueAt = today.minusDays(1).toString())
        insertCard(userId, deckId, front = "future", dueAt = today.plusDays(3).toString())

        val due = repo.getDueCardsForDeck(deckId, today)

        assertEquals(2, due.size)
        assertTrue(due.none { it.front == "future" })
    }

    // CRDREP5
    // Reviewing a new card with Good schedules it for tomorrow, advances the
    // SM-2 state, and writes a review-log row.
    @Test
    fun reviewCard_updatesScheduleAndWritesLog() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-review@example.com")
        val subjectId = insertAssignment(userId = userId)
        val deckId = insertDeck(userId = userId, assignmentId = subjectId)
        insertCard(userId, deckId, front = "Q", back = "A")

        val today = LocalDate.now()
        val card = repo.getCards(deckId).first()
        repo.reviewCard(card, SpacedRepetition.GOOD, today)

        val updated = repo.getCards(deckId).first()
        assertEquals(1, updated.intervalDays)
        assertEquals(1, updated.repetitions)
        assertEquals(today.plusDays(1).toString(), updated.dueAt)
        assertNotNull(updated.lastReviewedAt)
        assertEquals(1, db.reviewLogDao().getReviewTimestamps(userId).size)
    }

    // CRDREP6
    // After a review, the card is no longer in today's due list.
    @Test
    fun reviewCard_removesCardFromTodaysDueList() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-due-after@example.com")
        val subjectId = insertAssignment(userId = userId)
        val deckId = insertDeck(userId = userId, assignmentId = subjectId)
        insertCard(userId, deckId, dueAt = null)

        val today = LocalDate.now()
        repo.reviewCard(repo.getCards(deckId).first(), SpacedRepetition.GOOD, today)

        assertEquals(0, repo.getDueCardsForDeck(deckId, today).size)
    }
}


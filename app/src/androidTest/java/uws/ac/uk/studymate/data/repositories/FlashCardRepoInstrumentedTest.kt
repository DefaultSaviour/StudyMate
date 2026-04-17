package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class FlashCardRepoInstrumentedTest : RoomDbTestBase() {

    // FCRREP1
    // Save one flash card through the repository.
    // Check the chosen deck can load that saved card back.
    @Test
    fun addCard_savesTheCard() = runBlocking {
        val repo = FlashCardRepo(db)
        val userId = insertUser(email = "flashcard-repo-save@example.com")
        val subjectId = insertSubject(userId = userId, name = "German")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Words")

        repo.addCard(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = "Ja",
                back = "Yes"
            )
        )

        val cards = repo.getCards(deckId)

        assertEquals(1, cards.size)
        assertEquals("Ja", cards.first().front)
    }

    // FCRREP2
    // Update one saved flash card through the repository.
    // Make sure both the front and back text are changed.
    @Test
    fun updateCard_changesSavedValues() = runBlocking {
        val repo = FlashCardRepo(db)
        val userId = insertUser(email = "flashcard-repo-update@example.com")
        val subjectId = insertSubject(userId = userId, name = "Italian")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Basics")
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

    // FCRREP3
    // Delete one saved flash card through the repository.
    // Make sure the chosen deck becomes empty afterwards.
    @Test
    fun deleteCard_removesTheCard() = runBlocking {
        val repo = FlashCardRepo(db)
        val userId = insertUser(email = "flashcard-repo-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "Latin")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Basics")
        repo.addCard(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = "Salve",
                back = "Hello"
            )
        )

        val saved = repo.getCards(deckId).first()
        repo.deleteCard(saved)

        assertEquals(0, repo.getCards(deckId).size)
    }
}


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
        val subjectId = insertSubject(userId = userId, name = "French")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Basics")

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
    // Delete one saved card through the repository.
    // Make sure the chosen deck no longer has any cards left.
    @Test
    fun deleteCard_removesTheCard() = runBlocking {
        val repo = CardRepo(db)
        val userId = insertUser(email = "card-repo-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "Spanish")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Words")
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
}


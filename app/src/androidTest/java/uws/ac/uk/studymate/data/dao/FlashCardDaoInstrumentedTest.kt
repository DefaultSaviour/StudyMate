package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        val subjectId = insertSubject(userId = userId, name = "French")
        val firstDeckId = insertDeck(userId = userId, subjectId = subjectId, name = "Basics")
        val secondDeckId = insertDeck(userId = userId, subjectId = subjectId, name = "Advanced")

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
        val subjectId = insertSubject(userId = userId, name = "Spanish")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Words")
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
        val subjectId = insertSubject(userId = userId, name = "German")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Words")
        insertCard(userId = userId, deckId = deckId, front = "Ja", back = "Yes")

        val saved = db.cardDao().getCards(deckId).first()
        db.cardDao().delete(saved)

        assertEquals(0, db.cardDao().getCards(deckId).size)
    }
}


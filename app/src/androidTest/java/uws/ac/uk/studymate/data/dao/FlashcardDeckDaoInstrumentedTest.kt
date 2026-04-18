package uws.ac.uk.studymate.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
2/04/26
updated 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class FlashcardDeckDaoInstrumentedTest : RoomDbTestBase() {

    // DCKDAO1
    // Only return decks that belong to the chosen user.
    // This makes sure another user's decks are not mixed in.
    @Test
    fun getDecks_returnsOnlyThatUsersDecks() = runBlocking {
        val firstUserId = insertUser(email = "decks-one@example.com")
        val secondUserId = insertUser(email = "decks-two@example.com")
        val firstSubjectId = insertSubject(userId = firstUserId, name = "Maths")
        val secondSubjectId = insertSubject(userId = secondUserId, name = "Science")

        insertDeck(userId = firstUserId, subjectId = firstSubjectId, name = "Numbers")
        insertDeck(userId = firstUserId, subjectId = firstSubjectId, name = "Shapes")
        insertDeck(userId = secondUserId, subjectId = secondSubjectId, name = "Lab")

        val decks = db.deckDao().getDecks(firstUserId)

        assertEquals(2, decks.size)
        assertEquals(setOf("Numbers", "Shapes"), decks.map { it.name }.toSet())
    }

    // DCKDAO2
    // Load each deck with the cards saved inside it.
    // This checks the deck-to-card link works properly.
    @Test
    fun getDecksWithCards_returnsCardsInsideEachDeck() = runBlocking {
        val userId = insertUser(email = "deck-cards@example.com")
        val subjectId = insertSubject(userId = userId, name = "Geography")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Capitals")

        insertCard(userId = userId, deckId = deckId, front = "France", back = "Paris")
        insertCard(userId = userId, deckId = deckId, front = "Spain", back = "Madrid")

        val result = db.deckDao().getDecksWithCards(userId)

        assertEquals(1, result.size)
        assertEquals(2, result.first().cards.size)
    }

    // DCKDAO3
    // Delete a deck but keep the cards themselves.
    // The saved card should stay in the table with no deck id.
    @Test
    fun deleteDeck_keepsCardsButClearsTheirDeckId() = runBlocking {
        val userId = insertUser(email = "deck-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "History")
        val deckId = insertDeck(userId = userId, subjectId = subjectId, name = "Dates")
        insertCard(userId = userId, deckId = deckId, front = "1066", back = "Norman Conquest")

        val savedDeck = db.deckDao().getDecks(userId).first()
        val savedCard = db.cardDao().getCards(deckId).first()

        db.deckDao().delete(savedDeck)

        val cardsStillInDeck = db.cardDao().getCards(deckId)
        val cursor = db.query("SELECT deck_id FROM Flash_Cards WHERE id = ?", arrayOf(savedCard.id))
        cursor.moveToFirst()
        val deckIdWasCleared = cursor.isNull(0)
        cursor.close()

        assertTrue(cardsStillInDeck.isEmpty())
        assertNotNull(savedCard)
        assertTrue(deckIdWasCleared)
    }
}


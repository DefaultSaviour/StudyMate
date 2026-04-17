package uws.ac.uk.studymate.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.testutil.RoomDbTestBase
/*////////////////
Coded by Jamie Coleman
 13/04/26
 */////////////
@RunWith(AndroidJUnit4::class)
class DeckRepoInstrumentedTest : RoomDbTestBase() {

    // DCKREP1
    // Save one deck through the repository and keep its new id.
    // Check the saved deck name and id match what comes back.
    @Test
    fun addDeck_savesTheDeckAndReturnsItsId() = runBlocking {
        val repo = DeckRepo(db)
        val userId = insertUser(email = "deck-repo-save@example.com")
        val subjectId = insertSubject(userId = userId, name = "History")

        val deckId = repo.addDeck(
            FlashcardDeck(
                userId = userId,
                subjectId = subjectId,
                name = "Dates"
            )
        )

        val decks = repo.getDecks(userId)

        assertEquals(1, decks.size)
        assertEquals(deckId.toInt(), decks.first().id)
        assertEquals("Dates", decks.first().name)
    }

    // DCKREP2
    // Delete one deck through the repository.
    // Make sure that user's deck list becomes empty.
    @Test
    fun deleteDeck_removesTheDeck() = runBlocking {
        val repo = DeckRepo(db)
        val userId = insertUser(email = "deck-repo-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "Geography")
        repo.addDeck(
            FlashcardDeck(
                userId = userId,
                subjectId = subjectId,
                name = "Capitals"
            )
        )

        val saved = repo.getDecks(userId).first()
        repo.deleteDeck(saved)

        assertEquals(0, repo.getDecks(userId).size)
    }
}


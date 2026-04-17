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
class FlashcardDeckRepoInstrumentedTest : RoomDbTestBase() {

    // FDKREP1
    // Save one flashcard deck through the repository.
    // Check the new deck can be loaded back by name.
    @Test
    fun addDeck_savesTheDeck() = runBlocking {
        val repo = FlashcardDeckRepo(db)
        val userId = insertUser(email = "flashdeck-repo-save@example.com")
        val subjectId = insertSubject(userId = userId, name = "Chemistry")

        repo.addDeck(
            FlashcardDeck(
                userId = userId,
                subjectId = subjectId,
                name = "Formulas"
            )
        )

        val decks = repo.getDecks(userId)

        assertEquals(1, decks.size)
        assertEquals("Formulas", decks.first().name)
    }

    // FDKREP2
    // Update one saved deck through the repository.
    // Make sure the new deck name was stored.
    @Test
    fun updateDeck_changesSavedValues() = runBlocking {
        val repo = FlashcardDeckRepo(db)
        val userId = insertUser(email = "flashdeck-repo-update@example.com")
        val subjectId = insertSubject(userId = userId, name = "Physics")
        repo.addDeck(
            FlashcardDeck(
                userId = userId,
                subjectId = subjectId,
                name = "Forces"
            )
        )

        val saved = repo.getDecks(userId).first()
        repo.updateDeck(saved.copy(name = "Motion"))

        val updated = repo.getDecks(userId).first()

        assertEquals("Motion", updated.name)
    }

    // FDKREP3
    // Delete one saved deck through the repository.
    // Make sure the user's deck list becomes empty after the delete.
    @Test
    fun deleteDeck_removesTheDeck() = runBlocking {
        val repo = FlashcardDeckRepo(db)
        val userId = insertUser(email = "flashdeck-repo-delete@example.com")
        val subjectId = insertSubject(userId = userId, name = "Computing")
        repo.addDeck(
            FlashcardDeck(
                userId = userId,
                subjectId = subjectId,
                name = "Terms"
            )
        )

        val saved = repo.getDecks(userId).first()
        repo.deleteDeck(saved)

        assertEquals(0, repo.getDecks(userId).size)
    }
}


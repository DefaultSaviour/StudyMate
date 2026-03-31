package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashcardDeck

// Handles flashcard deck operations through the DAO.
class FlashcardDeckRepo(private val db: StudyMateDatabase) {

    // Save a new deck to the database.
    suspend fun addDeck(deck: FlashcardDeck) {
        db.deckDao().insert(deck)
    }

    // Update an existing deck's details.
    suspend fun updateDeck(deck: FlashcardDeck) {
        db.deckDao().update(deck)
    }

    // Remove a deck from the database.
    suspend fun deleteDeck(deck: FlashcardDeck) {
        db.deckDao().delete(deck)
    }

    // Get all decks that belong to a specific user.
    suspend fun getDecks(userId: Int) =
        db.deckDao().getDecks(userId)
}

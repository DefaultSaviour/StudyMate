package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashcardDeck
/*//////////////////////
Coded by Jamie Coleman
 12/03/26
updated 17/04/26
 *//////////////////////
// Handles flashcard deck operations through the DAO.
class FlashcardDeckRepo(private val db: StudyMateDatabase) {

    // Save a new deck to the database and return its generated ID.
    suspend fun addDeck(deck: FlashcardDeck): Long {
        return db.deckDao().insert(deck)
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

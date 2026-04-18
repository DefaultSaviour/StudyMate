package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashcardDeck

/*//////////////////////
Coded by Jamie Coleman
 12/03/26
 *//////////////////////
// Handles deck-related database operations through the DAO.
class DeckRepo(private val db: StudyMateDatabase) {

    // Save a new deck and return its generated ID.
    suspend fun addDeck(deck: FlashcardDeck): Long = db.deckDao().insert(deck)

    // Update an existing deck's details.
    suspend fun updateDeck(deck: FlashcardDeck) = db.deckDao().update(deck)

    // Get all decks that belong to a specific user.
    suspend fun getDecks(userId: Int) = db.deckDao().getDecks(userId)

    // Remove a deck from the database.
    suspend fun deleteDeck(deck: FlashcardDeck) = db.deckDao().delete(deck)
}

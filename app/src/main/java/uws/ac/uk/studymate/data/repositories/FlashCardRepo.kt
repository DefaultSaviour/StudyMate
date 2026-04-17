package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard
/*//////////////////////
Coded by Jamie Coleman
 11/03/26
 *//////////////////////
// Handles flashcard-related database operations through the DAO.
class FlashCardRepo(private val db: StudyMateDatabase) {

    // Save a new flashcard to the database.
    suspend fun addCard(card: FlashCard) {
        db.cardDao().insert(card)
    }

    // Update an existing flashcard's content.
    suspend fun updateCard(card: FlashCard) {
        db.cardDao().update(card)
    }

    // Remove a flashcard from the database.
    suspend fun deleteCard(card: FlashCard) {
        db.cardDao().delete(card)
    }

    // Get all flashcards that belong to a specific deck.
    suspend fun getCards(deckId: Int) =
        db.cardDao().getCards(deckId)
}

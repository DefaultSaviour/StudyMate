package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard

// Handles flashcard-related database operations through the DAO.
class CardRepo(private val db: StudyMateDatabase) {

    // Save a new flashcard to the database.
    suspend fun addCard(card: FlashCard) = db.cardDao().insert(card)

    // Get all flashcards that belong to a specific deck.
    suspend fun getCards(deckId: Int) = db.cardDao().getCards(deckId)

    // Remove a flashcard from the database.
    suspend fun deleteCard(card: FlashCard) = db.cardDao().delete(card)
}

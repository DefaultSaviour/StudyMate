package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard

class FlashCardRepo(private val db: StudyMateDatabase) {

    suspend fun addCard(card: FlashCard) {
        db.cardDao().insert(card)
    }

    suspend fun updateCard(card: FlashCard) {
        db.cardDao().update(card)
    }

    suspend fun deleteCard(card: FlashCard) {
        db.cardDao().delete(card)
    }

    suspend fun getCards(deckId: Int) =
        db.cardDao().getCards(deckId)
}

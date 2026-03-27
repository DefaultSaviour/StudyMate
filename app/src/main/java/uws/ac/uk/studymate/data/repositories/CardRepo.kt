package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard

class CardRepo(private val db: StudyMateDatabase) {

    suspend fun addCard(card: FlashCard) = db.cardDao().insert(card)

    suspend fun getCards(deckId: Int) = db.cardDao().getCards(deckId)

    suspend fun deleteCard(card: FlashCard) = db.cardDao().delete(card)
}

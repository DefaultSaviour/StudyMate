package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashcardDeck

class FlashcardDeckRepo(private val db: StudyMateDatabase) {

    suspend fun addDeck(deck: FlashcardDeck) {
        db.deckDao().insert(deck)
    }

    suspend fun updateDeck(deck: FlashcardDeck) {
        db.deckDao().update(deck)
    }

    suspend fun deleteDeck(deck: FlashcardDeck) {
        db.deckDao().delete(deck)
    }

    suspend fun getDecks(userId: Int) =
        db.deckDao().getDecks(userId)
}

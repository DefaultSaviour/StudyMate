package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashcardDeck

class DeckRepo(private val db: StudyMateDatabase) {

    suspend fun addDeck(deck: FlashcardDeck): Long = db.deckDao().insert(deck)

    suspend fun getDecks(userId: Int) = db.deckDao().getDecks(userId)

    suspend fun deleteDeck(deck: FlashcardDeck) = db.deckDao().delete(deck)
}

// we should disguss if we want to edit them but that should be for v2 or whatever
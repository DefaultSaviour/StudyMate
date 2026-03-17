package uws.ac.uk.studymate.data.repositories

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck

data class DeckWithCards(
    @Embedded val deck: FlashcardDeck,
    @Relation(parentColumn = "id", entityColumn = "deck_id")
    val cards: List<FlashCard>
)
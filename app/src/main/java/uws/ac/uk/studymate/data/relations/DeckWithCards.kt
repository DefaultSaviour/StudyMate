package uws.ac.uk.studymate.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
/*//////////////////////
Coded by Jamie Coleman
 24/03/26
 *//////////////////////
// Joins a flashcard deck with all the cards inside it.
// Room fills in the cards list automatically using the relationship.
data class DeckWithCards(
    @Embedded val deck: FlashcardDeck,                          // The deck itself.
    @Relation(parentColumn = "id", entityColumn = "deck_id")
    val cards: List<FlashCard>                                  // All cards that belong to this deck.
)


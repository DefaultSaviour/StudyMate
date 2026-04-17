package uws.ac.uk.studymate.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.Subject
/*//////////////////////
Coded by Jamie Coleman
 26/03/26
 *//////////////////////
// Joins a subject with all the flashcard decks that belong to it.
// Room fills in the decks list automatically using the relationship.
data class SubjectWithDecks(
    @Embedded val subject: Subject,                                    // The subject itself.
    @Relation(parentColumn = "id", entityColumn = "subject_id")
    val decks: List<FlashcardDeck>                                     // All flashcard decks under this subject.
)


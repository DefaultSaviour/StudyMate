package uws.ac.uk.studymate.data.entities

import androidx.room.*
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 *//////////////////////
// Represents one flashcard in the Flash_Cards table.
// Each card belongs to a user and optionally to a deck.
// Deleting the user removes the card; deleting the deck sets deck_id to null.
@Entity(
    tableName = "Flash_Cards",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FlashcardDeck::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FlashCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,               // Auto-generated unique ID.
    @ColumnInfo(name = "user_id", index = true) val userId: Int,  // The user who owns this card.
    @ColumnInfo(name = "deck_id", index = true) val deckId: Int?, // The deck this card belongs to, or null if unassigned.
    val front: String,                                            // The question or prompt shown on the front.
    val back: String                                              // The answer shown on the back.
)

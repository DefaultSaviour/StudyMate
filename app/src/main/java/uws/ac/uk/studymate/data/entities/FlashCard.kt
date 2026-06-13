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
    val back: String,                                             // The answer shown on the back.

    // ── Spaced-repetition (SM-2) scheduling state ──
    @ColumnInfo(name = "ease_factor") val easeFactor: Double = 2.5,       // SM-2 ease; starts at 2.5, floor 1.3.
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 0,        // Current gap (days) between reviews.
    @ColumnInfo(name = "repetitions") val repetitions: Int = 0,          // Consecutive successful reviews.
    @ColumnInfo(name = "due_at") val dueAt: String? = null,            // ISO LocalDate when next due; null = due now (new card).
    @ColumnInfo(name = "last_reviewed_at") val lastReviewedAt: String? = null // ISO instant of the last review.
)

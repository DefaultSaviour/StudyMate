package uws.ac.uk.studymate.data.entities

import androidx.room.*
/*//////////////////////
One row per flashcard review. Powers the statistics screen (cards reviewed
per day, study streak) — the card's own fields only hold its *current* SM-2
state, so a separate log is needed for review history.

Deleting the user removes their logs (CASCADE). Deleting a card keeps the log
but nulls card_id (SET_NULL), so historical review counts survive card edits.
 *//////////////////////
@Entity(
    tableName = "Review_Logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FlashCard::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id", index = true) val userId: Int,        // Who did the review.
    @ColumnInfo(name = "card_id", index = true) val cardId: Int?,       // The card reviewed, or null if it was later deleted.
    @ColumnInfo(name = "reviewed_at") val reviewedAt: String,           // ISO instant the review happened.
    val grade: Int                                                      // SpacedRepetition grade: 0=Again,1=Hard,2=Good,3=Easy.
)

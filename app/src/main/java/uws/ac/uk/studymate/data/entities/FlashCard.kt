package uws.ac.uk.studymate.data.entities

import androidx.room.*
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id", index = true) val userId: Int,
    @ColumnInfo(name = "deck_id", index = true) val deckId: Int?,
    val front: String,
    val back: String
)

package uws.ac.uk.studymate.data.entities
import androidx.room.*
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 *//////////////////////
// Represents one flashcard deck in the Flashcard_Decks table.
// Each deck belongs to a user and a subject.
// Deleting the user or the subject automatically removes the deck.
@Entity(
    tableName = "Flashcard_Decks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FlashcardDeck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,            // Auto-generated unique ID.
    @ColumnInfo(name = "user_id", index = true) val userId: Int,       // The user who owns this deck.
    @ColumnInfo(name = "subject_id", index = true) val subjectId: Int, // The subject this deck belongs to.
    val name: String                                                   // The display name of the deck.
)

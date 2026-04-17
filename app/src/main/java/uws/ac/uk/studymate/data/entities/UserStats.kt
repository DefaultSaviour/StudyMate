package uws.ac.uk.studymate.data.entities
import androidx.room.*
/*//////////////////////
Coded by Jamie Coleman
 11/03/26
 *//////////////////////
// Represents the stats for one user in the User_Stats table.
// Tracks counts like assignments, flashcards, and study streak days.
// Deleting the user automatically removes their stats.
@Entity(
    tableName = "User_Stats",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserStats(
    @PrimaryKey @ColumnInfo(name = "user_id", index = true) val userId: Int, // The user these stats belong to (also the primary key).
    @ColumnInfo(name = "assignments_count") val assignmentsCount: Int = 0,   // How many assignments the user has.
    @ColumnInfo(name = "flashcards_count") val flashcardsCount: Int = 0,     // How many flashcards the user has.
    @ColumnInfo(name = "streak_days") val streakDays: Int = 0                // How many consecutive days the user has studied.
)

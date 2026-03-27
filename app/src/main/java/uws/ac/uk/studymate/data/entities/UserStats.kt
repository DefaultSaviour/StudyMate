package uws.ac.uk.studymate.data.entities
import androidx.room.*

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
    @PrimaryKey @ColumnInfo(name = "user_id", index = true) val userId: Int,
    @ColumnInfo(name = "assignments_count") val assignmentsCount: Int = 0,
    @ColumnInfo(name = "flashcards_count") val flashcardsCount: Int = 0,
    @ColumnInfo(name = "streak_days") val streakDays: Int = 0
)

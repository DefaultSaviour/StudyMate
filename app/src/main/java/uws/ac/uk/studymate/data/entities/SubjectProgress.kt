package uws.ac.uk.studymate.data.entities
import androidx.room.*

// Represents one progress record in the Subject_Progress table.
// Tracks how many tasks a user has completed out of the total for a subject.
// Deleting the user or the subject automatically removes the record.
// The combination of user_id and subject_id must be unique.
@Entity(
    tableName = "Subject_Progress",
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
    ],
    indices = [Index(value = ["user_id", "subject_id"], unique = true)]
)
data class SubjectProgress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,                        // Auto-generated unique ID.
    @ColumnInfo(name = "user_id", index = true) val userId: Int,             // The user this progress belongs to.
    @ColumnInfo(name = "subject_id" , index = true) val subjectId: Int,      // The subject being tracked.
    @ColumnInfo(name = "completed_tasks") val completedTasks: Int = 0,       // How many tasks the user has finished.
    @ColumnInfo(name = "total_tasks") val totalTasks: Int = 0                // How many tasks the subject has in total.
)

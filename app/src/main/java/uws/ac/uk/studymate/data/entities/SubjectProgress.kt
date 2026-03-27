package uws.ac.uk.studymate.data.entities
import androidx.room.*

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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id", index = true) val userId: Int,
    @ColumnInfo(name = "subject_id" , index = true) val subjectId: Int,
    @ColumnInfo(name = "completed_tasks") val completedTasks: Int = 0,
    @ColumnInfo(name = "total_tasks") val totalTasks: Int = 0
)

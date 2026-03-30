package uws.ac.uk.studymate.data.entities

import androidx.room.*
        
@Entity(
        tableName = "Assignments",
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
data class Assignment(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        @ColumnInfo(name = "user_id", index = true) val userId: Int,
        @ColumnInfo(name = "subject_id", index = true) val subjectId: Int,
        val title: String,
        @ColumnInfo(name = "due_date") val dueDate: String?
)

package uws.ac.uk.studymate.data.entities

import androidx.room.*

// Represents one assignment in the Assignments table.
// Each assignment belongs to a user and a subject.
// Deleting the user or the subject automatically removes the assignment.
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
        @PrimaryKey(autoGenerate = true) val id: Int = 0,        // Auto-generated unique ID.
        @ColumnInfo(name = "user_id", index = true) val userId: Int,     // The user who owns this assignment.
        @ColumnInfo(name = "subject_id", index = true) val subjectId: Int, // The subject this assignment belongs to.
        val title: String,                                                // The name of the assignment.
        @ColumnInfo(name = "due_date") val dueDate: String?               // Optional due date for the assignment.
)

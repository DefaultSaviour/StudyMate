package uws.ac.uk.studymate.data.entities

import androidx.room.*

// Represents one subject in the Subjects table.
// Each subject belongs to a user.
// Deleting the user automatically removes all their subjects.
@Entity(
    tableName = "Subjects",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,            // Auto-generated unique ID.
    @ColumnInfo(name = "user_id", index = true) val userId: Int,       // The user who owns this subject.
    val name: String,                                                  // The display name of the subject.
    val color: String?                                                 // Optional color used to highlight the subject in the UI.
)

package uws.ac.uk.studymate.data.entities

import androidx.room.*
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 updated 7/04/26
 updated 17/06/26 — merged Subject into Assignment (flat model: name + colour live here now)
 *//////////////////////
// Represents one assignment in the Assignments table.
//
// This is now the single top-level study item: it carries its own colour (the
// old Subject concept folded in) and owns its flashcard decks directly. There is
// no Subject table any more. Each assignment belongs to a user; deleting the user
// removes all their assignments (and, via cascade, their decks/cards).
@Entity(
    tableName = "Assignments",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,                   // Auto-generated unique ID.
    @ColumnInfo(name = "user_id", index = true) val userId: Int,       // The user who owns this assignment.
    val title: String,                                                 // The display name (e.g. "Maths - Calculus").
    val color: String?,                                                // Optional colour used to highlight it in the UI.
    @ColumnInfo(name = "due_date") val dueDate: String?,              // Saved due date and time (required at the UI level).
    @ColumnInfo(name = "icon") val icon: String = "assignment",       // Saved icon key that the UI uses later.
    @ColumnInfo(name = "completed_at") val completedAt: String? = null // ISO instant when marked done; null = not done.
)

package uws.ac.uk.studymate.data.entities

import androidx.room.*
/*//////////////////////
One logged focus block (0.9J) — focused seconds from the Pomodoro timer,
optionally tied to the assignment the user was studying. Powers the
"Focused today / this week" statistics.

Derived history (like Review_Logs), so it is NOT included in backups.
Deleting the user removes their sessions (CASCADE). Deleting the assignment
keeps the session but nulls assignment_id (SET_NULL), so totals survive.
 *//////////////////////
@Entity(
    tableName = "Focus_Sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Assignment::class,
            parentColumns = ["id"],
            childColumns = ["assignment_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id", index = true) val userId: Int,                 // Who studied.
    @ColumnInfo(name = "assignment_id", index = true) val assignmentId: Int?,    // Studied assignment, or null (none / since deleted).
    @ColumnInfo(name = "focused_seconds") val focusedSeconds: Int,               // Focused time only (breaks excluded).
    @ColumnInfo(name = "ended_at") val endedAt: String                          // ISO instant the block ended.
)

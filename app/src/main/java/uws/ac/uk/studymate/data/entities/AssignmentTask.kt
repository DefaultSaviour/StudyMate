package uws.ac.uk.studymate.data.entities

import androidx.room.*
/*//////////////////////
One checklist item under an assignment (0.9J).
Lets a user break an assignment into to-do steps, ticked off either in the
Assignments checklist panel or live in the focus timer — both write this table,
so the two screens always agree.

Deleting the user or the parent assignment removes its tasks (CASCADE).
 *//////////////////////
@Entity(
    tableName = "Assignment_Tasks",
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
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AssignmentTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id", index = true) val userId: Int,             // Owner.
    @ColumnInfo(name = "assignment_id", index = true) val assignmentId: Int, // Parent assignment.
    val text: String,                                                        // The checklist item text.
    @ColumnInfo(name = "is_done") val isDone: Boolean = false,              // Ticked off?
    val position: Int = 0,                                                   // Sort order within the assignment.
    @ColumnInfo(name = "created_at") val createdAt: String? = null          // ISO instant created.
)

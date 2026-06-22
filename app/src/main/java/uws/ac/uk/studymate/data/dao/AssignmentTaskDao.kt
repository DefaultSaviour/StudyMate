package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.AssignmentTask
/*//////////////////////
Operations on the Assignment_Tasks table (0.9J) — checklist items under an
assignment, ordered by position.
 *//////////////////////
@Dao
interface AssignmentTaskDao {
    // Save a new item and return its generated id.
    @Insert suspend fun insert(task: AssignmentTask): Long
    // Update an existing item.
    @Update suspend fun update(task: AssignmentTask)
    // Remove an item.
    @Delete suspend fun delete(task: AssignmentTask)

    // All checklist items for an assignment, in display order.
    @Query("SELECT * FROM Assignment_Tasks WHERE assignment_id = :assignmentId ORDER BY position ASC, id ASC")
    suspend fun getForAssignment(assignmentId: Int): List<AssignmentTask>

    // Set the done flag for one item (used by the checkbox toggle).
    @Query("UPDATE Assignment_Tasks SET is_done = :done WHERE id = :taskId")
    suspend fun setDone(taskId: Int, done: Boolean)

    // Highest position currently used under an assignment, or -1 if none — so a
    // new item appends at maxPosition + 1.
    @Query("SELECT COALESCE(MAX(position), -1) FROM Assignment_Tasks WHERE assignment_id = :assignmentId")
    suspend fun maxPosition(assignmentId: Int): Int

    // Counts backing the "N of M done" progress label.
    @Query("SELECT COUNT(*) FROM Assignment_Tasks WHERE assignment_id = :assignmentId")
    suspend fun countForAssignment(assignmentId: Int): Int

    @Query("SELECT COUNT(*) FROM Assignment_Tasks WHERE assignment_id = :assignmentId AND is_done = 1")
    suspend fun countDoneForAssignment(assignmentId: Int): Int

    // One row per assignment with task counts, so the Assignments list can show a
    // "done/total" hint without a query per row.
    @Query(
        "SELECT assignment_id AS assignmentId, COUNT(*) AS total, " +
            "COALESCE(SUM(is_done), 0) AS done " +
            "FROM Assignment_Tasks WHERE user_id = :userId GROUP BY assignment_id"
    )
    suspend fun progressForUser(userId: Int): List<TaskProgress>
}

// Per-assignment checklist progress projection (done out of total).
data class TaskProgress(
    val assignmentId: Int,
    val done: Int,
    val total: Int
)

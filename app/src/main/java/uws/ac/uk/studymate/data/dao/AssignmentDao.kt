package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.Assignment
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 *//////////////////////
// Provides database operations for the Assignments table.
@Dao
interface AssignmentDao {
    // Save a new assignment and return the generated row id.
    @Insert suspend fun insert(assignment: Assignment): Long
    // Update an existing assignment's details.
    @Update suspend fun update(assignment: Assignment)
    // Remove an assignment from the database.
    @Delete suspend fun delete(assignment: Assignment)

    // Get all assignments for a user, sorted by the earliest due date first.
    @Query("SELECT * FROM Assignments WHERE user_id = :userId ORDER BY due_date ASC")
    suspend fun getAssignments(userId: Int): List<Assignment>

    // Get all assignments belonging to one subject. Used by the backup export.
    @Query("SELECT * FROM Assignments WHERE subject_id = :subjectId")
    suspend fun getAssignmentsForSubject(subjectId: Int): List<Assignment>

    // Look up a single assignment by ID. Used by the notification worker to
    // verify the assignment still exists at fire time.
    @Query("SELECT * FROM Assignments WHERE id = :id")
    suspend fun getById(id: Int): Assignment?

    // Mark an assignment done (ISO instant) or not done (null).
    @Query("UPDATE Assignments SET completed_at = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Int, completedAt: String?)
}

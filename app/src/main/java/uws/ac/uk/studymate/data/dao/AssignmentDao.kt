package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.Assignment

// Provides database operations for the Assignments table.
@Dao
interface AssignmentDao {
    // Save a new assignment to the database.
    @Insert suspend fun insert(assignment: Assignment)
    // Update an existing assignment's details.
    @Update suspend fun update(assignment: Assignment)
    // Remove an assignment from the database.
    @Delete suspend fun delete(assignment: Assignment)

    // Get all assignments for a user, sorted by the earliest due date first.
    @Query("SELECT * FROM Assignments WHERE user_id = :userId ORDER BY due_date ASC")
    suspend fun getAssignments(userId: Int): List<Assignment>
}

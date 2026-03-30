package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.Assignment
@Dao
interface AssignmentDao {
    @Insert suspend fun insert(assignment: Assignment)
    @Update suspend fun update(assignment: Assignment)
    @Delete suspend fun delete(assignment: Assignment)

    @Query("SELECT * FROM Assignments WHERE user_id = :userId ORDER BY due_date ASC")
    suspend fun getAssignments(userId: Int): List<Assignment>
}

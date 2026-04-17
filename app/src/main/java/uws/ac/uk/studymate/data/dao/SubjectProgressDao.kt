package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.SubjectProgress
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 *//////////////////////
// Provides database operations for the Subject_Progress table.
@Dao
interface SubjectProgressDao {
    // Save a new progress record for a subject.
    @Insert suspend fun insert(progress: SubjectProgress)
    // Update an existing progress record.
    @Update suspend fun update(progress: SubjectProgress)

    // Get all progress records that belong to a specific user.
    @Query("SELECT * FROM Subject_Progress WHERE user_id = :userId")
    suspend fun getAll(userId: Int): List<SubjectProgress>
}

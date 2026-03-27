package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.SubjectProgress

@Dao
interface SubjectProgressDao {
    @Insert suspend fun insert(progress: SubjectProgress)
    @Update suspend fun update(progress: SubjectProgress)

    @Query("SELECT * FROM Subject_Progress WHERE user_id = :userId")
    suspend fun getAll(userId: Int): List<SubjectProgress>
}

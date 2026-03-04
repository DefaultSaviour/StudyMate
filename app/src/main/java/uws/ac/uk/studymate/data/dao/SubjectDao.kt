package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.Subject
@Dao

interface SubjectDao {
    @Insert suspend fun insert(subject: Subject)
    @Update suspend fun update(subject: Subject)
    @Delete suspend fun delete(subject: Subject)

    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjects(userId: Int): List<Subject>
}

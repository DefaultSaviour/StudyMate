package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.SubjectWithProgress

@Dao

interface SubjectDao {
    @Insert suspend fun insert(subject: Subject): Long
    @Update suspend fun update(subject: Subject)
    @Delete suspend fun delete(subject: Subject)

    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjects(userId: Int): List<Subject>



    //////// JOIN ///////////////////////////////////////
    @Transaction
    @Query("SELECT * FROM Subjects WHERE user_id = :userId")
    suspend fun getSubjectsWithProgress(userId: Int): List<SubjectWithProgress>
    //////// JOIN ///////////////////////////////////////


}

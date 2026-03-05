package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.User
@Dao
interface UserDao {
    ///////////////////// for testing only /////////////////////
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>
    ///////////////////// for testing only /////////////////////

    @Insert suspend fun insert(user: User)
    @Update suspend fun update(user: User)
    @Delete suspend fun delete(user: User)

    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getById(id: Int): User?
}

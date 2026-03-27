package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.UserStats
@Dao
interface UserStatsDao {
    @Insert suspend fun insert(stats: UserStats)
    @Update suspend fun update(stats: UserStats)

    @Query("SELECT * FROM User_Stats WHERE user_id = :userId")
    suspend fun get(userId: Int): UserStats?
}

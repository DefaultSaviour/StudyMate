package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.UserStats

// Provides database operations for the User_Stats table.
@Dao
interface UserStatsDao {
    // Save a new stats record for a user.
    @Insert suspend fun insert(stats: UserStats)
    // Update an existing stats record.
    @Update suspend fun update(stats: UserStats)

    // Get the stats for a specific user, or return null if none exist.
    @Query("SELECT * FROM User_Stats WHERE user_id = :userId")
    suspend fun get(userId: Int): UserStats?
}

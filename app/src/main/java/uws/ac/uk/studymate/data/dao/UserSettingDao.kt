package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.UserSettings

// Provides database operations for the User_Settings table.
@Dao
interface UserSettingsDao {
    // Save a new settings record for a user.
    @Insert suspend fun insert(settings: UserSettings)
    // Update an existing settings record.
    @Update suspend fun update(settings: UserSettings)

    // Get the settings for a specific user, or return null if none exist.
    @Query("SELECT * FROM User_Settings WHERE user_id = :userId")
    suspend fun get(userId: Int): UserSettings?
}

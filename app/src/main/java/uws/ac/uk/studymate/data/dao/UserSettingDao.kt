package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.UserSettings

@Dao
interface UserSettingsDao {
    @Insert suspend fun insert(settings: UserSettings)
    @Update suspend fun update(settings: UserSettings)

    @Query("SELECT * FROM User_Settings WHERE user_id = :userId")
    suspend fun get(userId: Int): UserSettings?
}


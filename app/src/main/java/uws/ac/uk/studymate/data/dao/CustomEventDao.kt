package uws.ac.uk.studymate.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import uws.ac.uk.studymate.data.entities.CustomEvent

@Dao
interface CustomEventDao {
    @Insert
    suspend fun insert(event: CustomEvent)

    @Delete
    suspend fun delete(event: CustomEvent)

    // ISO dates sort lexicographically, so we can filter and sort by date directly
    @Query("SELECT * FROM Custom_Events WHERE user_id = :userId ORDER BY date ASC")
    suspend fun getEventsForUser(userId: Int): List<CustomEvent>
}

package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.FocusSession
/*//////////////////////
Operations on the Focus_Sessions table (0.9J) — logged focus blocks that power
the "Focused today / this week" statistics.
 *//////////////////////
@Dao
interface FocusSessionDao {
    // Record one completed focus block.
    @Insert suspend fun insert(session: FocusSession)

    // Total focused seconds at or after the given ISO instant (today / this week).
    // ISO-8601 instants sort lexicographically, so a string >= compare works.
    @Query("SELECT COALESCE(SUM(focused_seconds), 0) FROM Focus_Sessions WHERE user_id = :userId AND ended_at >= :sinceIso")
    suspend fun sumFocusedSecondsSince(userId: Int, sinceIso: String): Int

    // Lifetime count of logged focus sessions — feeds the "Sprinter" trophy (1.2).
    @Query("SELECT COUNT(*) FROM Focus_Sessions WHERE user_id = :userId")
    suspend fun countAll(userId: Int): Int
}

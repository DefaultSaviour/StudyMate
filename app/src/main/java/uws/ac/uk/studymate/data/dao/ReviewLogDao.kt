package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.ReviewLog
/*//////////////////////
Operations on the Review_Logs table — one row per flashcard review. Powers the
statistics screen (reviews per day, study streak).
 *//////////////////////
@Dao
interface ReviewLogDao {
    // Record one review.
    @Insert suspend fun insert(log: ReviewLog)

    // How many reviews the user has done at or after the given ISO instant.
    // ISO-8601 instants sort lexicographically, so a string >= compare works.
    @Query("SELECT COUNT(*) FROM Review_Logs WHERE user_id = :userId AND reviewed_at >= :sinceIso")
    suspend fun countReviewsSince(userId: Int, sinceIso: String): Int

    // Every review timestamp for the user, newest first — used to compute the streak.
    @Query("SELECT reviewed_at FROM Review_Logs WHERE user_id = :userId ORDER BY reviewed_at DESC")
    suspend fun getReviewTimestamps(userId: Int): List<String>
}

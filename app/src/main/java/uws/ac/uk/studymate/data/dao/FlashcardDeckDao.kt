package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.FlashcardDeck
@Dao
interface FlashcardDeckDao {
    @Insert suspend fun insert(deck: FlashcardDeck)
    @Update suspend fun update(deck: FlashcardDeck)
    @Delete suspend fun delete(deck: FlashcardDeck)

    @Query("SELECT * FROM Flashcard_Decks WHERE user_id = :userId")
    suspend fun getDecks(userId: Int): List<FlashcardDeck>
}

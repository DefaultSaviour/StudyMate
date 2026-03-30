package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.FlashCard
@Dao
interface FlashCardDao {
    @Insert suspend fun insert(card: FlashCard)
    @Update suspend fun update(card: FlashCard)
    @Delete suspend fun delete(card: FlashCard)

    @Query("SELECT * FROM Flash_Cards WHERE deck_id = :deckId")
    suspend fun getCards(deckId: Int): List<FlashCard>
}

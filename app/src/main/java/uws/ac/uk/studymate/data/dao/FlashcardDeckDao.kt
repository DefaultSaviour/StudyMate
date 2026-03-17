package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.repositories.DeckWithCards

@Dao
interface FlashcardDeckDao {
    @Insert suspend fun insert(deck: FlashcardDeck): Long
    @Update suspend fun update(deck: FlashcardDeck)
    @Delete suspend fun delete(deck: FlashcardDeck)

    @Query("SELECT * FROM Flashcard_Decks WHERE user_id = :userId")
    suspend fun getDecks(userId: Int): List<FlashcardDeck>


    //////// JOIN ///////////////////////////////////////
    @Transaction
    @Query("SELECT * FROM Flashcard_Decks WHERE user_id = :userId")
    suspend fun getDecksWithCards(userId: Int): List<DeckWithCards>
    //////// JOIN ///////////////////////////////////////

}




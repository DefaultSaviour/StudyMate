package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.FlashCard
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 updated 17/04/26
 *//////////////////////
// Provides database operations for the Flash_Cards table.
@Dao
interface FlashCardDao {
    // Save a new flashcard to the database.
    @Insert suspend fun insert(card: FlashCard)
    // Update an existing flashcard's content.
    @Update suspend fun update(card: FlashCard)
    // Remove a flashcard from the database.
    @Delete suspend fun delete(card: FlashCard)

    // Get all flashcards that belong to a specific deck.
    @Query("SELECT * FROM Flash_Cards WHERE deck_id = :deckId")
    suspend fun getCards(deckId: Int): List<FlashCard>

    // Remove every flashcard that belongs to a specific deck.
    @Query("DELETE FROM Flash_Cards WHERE deck_id = :deckId")
    suspend fun deleteCardsByDeck(deckId: Int)
}

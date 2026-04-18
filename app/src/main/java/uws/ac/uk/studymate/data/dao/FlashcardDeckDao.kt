package uws.ac.uk.studymate.data.dao
import androidx.room.*
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.relations.DeckWithCards
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 *//////////////////////
// Provides database operations for the Flashcard_Decks table.
@Dao
interface FlashcardDeckDao {
    // Save a new deck and return its generated ID.
    @Insert suspend fun insert(deck: FlashcardDeck): Long
    // Update an existing deck's details.
    @Update suspend fun update(deck: FlashcardDeck)
    // Remove a deck from the database.
    @Delete suspend fun delete(deck: FlashcardDeck)

    // Get all decks that belong to a specific user.
    @Query("SELECT * FROM Flashcard_Decks WHERE user_id = :userId")
    suspend fun getDecks(userId: Int): List<FlashcardDeck>

    // Get each deck together with all the flashcards inside it for a user.
    @Transaction
    @Query("SELECT * FROM Flashcard_Decks WHERE user_id = :userId")
    suspend fun getDecksWithCards(userId: Int): List<DeckWithCards>
}

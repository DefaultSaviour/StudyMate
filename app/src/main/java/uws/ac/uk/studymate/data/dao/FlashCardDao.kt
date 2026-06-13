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

    // ── Spaced repetition ──
    // `today` is an ISO LocalDate string (yyyy-MM-dd); ISO dates sort
    // lexicographically, so a string <= comparison is a correct date comparison.
    // A null due_at means the card is brand-new and therefore due now.

    // Cards in one deck that are due for review.
    @Query("SELECT * FROM Flash_Cards WHERE deck_id = :deckId AND (due_at IS NULL OR due_at <= :today)")
    suspend fun getDueCardsForDeck(deckId: Int, today: String): List<FlashCard>

    // All of a user's due cards across every deck.
    @Query("SELECT * FROM Flash_Cards WHERE user_id = :userId AND (due_at IS NULL OR due_at <= :today)")
    suspend fun getDueCards(userId: Int, today: String): List<FlashCard>

    @Query("SELECT COUNT(*) FROM Flash_Cards WHERE user_id = :userId AND (due_at IS NULL OR due_at <= :today)")
    suspend fun countDue(userId: Int, today: String): Int

    @Query("SELECT COUNT(*) FROM Flash_Cards WHERE user_id = :userId")
    suspend fun countAll(userId: Int): Int

    @Query("SELECT COUNT(*) FROM Flash_Cards WHERE user_id = :userId AND interval_days >= :matureInterval")
    suspend fun countMature(userId: Int, matureInterval: Int): Int
}

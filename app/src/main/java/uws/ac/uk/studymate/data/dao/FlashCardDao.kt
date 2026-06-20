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

    // The soonest future due date across a user's cards (ISO yyyy-MM-dd), or null
    // if nothing is scheduled ahead. Used to schedule the next review reminder.
    @Query("SELECT MIN(due_at) FROM Flash_Cards WHERE user_id = :userId AND due_at IS NOT NULL AND due_at > :today")
    suspend fun getNextDueDate(userId: Int, today: String): String?

    // ── Active-assignment variants ──
    // Once an assignment is finished — manually marked done (`completed_at`) OR its
    // due date has passed (`due_date < nowIso`) — its decks drop out of review:
    // no quick-review on the dashboard and no "cards due" reminders. These mirror
    // the queries above but JOIN to the owning assignment and keep only active ones.
    // ISO datetime strings sort lexicographically, so the string comparisons are
    // correct date/time comparisons. `today` is a LocalDate; `nowIso` a LocalDateTime.

    @Query(
        """
        SELECT c.* FROM Flash_Cards c
        JOIN Flashcard_Decks d ON c.deck_id = d.id
        JOIN Assignments a ON d.assignment_id = a.id
        WHERE c.user_id = :userId
          AND (c.due_at IS NULL OR c.due_at <= :today)
          AND a.completed_at IS NULL
          AND (a.due_date IS NULL OR a.due_date >= :nowIso)
        """
    )
    suspend fun getDueCardsActive(userId: Int, today: String, nowIso: String): List<FlashCard>

    @Query(
        """
        SELECT COUNT(*) FROM Flash_Cards c
        JOIN Flashcard_Decks d ON c.deck_id = d.id
        JOIN Assignments a ON d.assignment_id = a.id
        WHERE c.user_id = :userId
          AND (c.due_at IS NULL OR c.due_at <= :today)
          AND a.completed_at IS NULL
          AND (a.due_date IS NULL OR a.due_date >= :nowIso)
        """
    )
    suspend fun countDueActive(userId: Int, today: String, nowIso: String): Int

    @Query(
        """
        SELECT MIN(c.due_at) FROM Flash_Cards c
        JOIN Flashcard_Decks d ON c.deck_id = d.id
        JOIN Assignments a ON d.assignment_id = a.id
        WHERE c.user_id = :userId AND c.due_at IS NOT NULL AND c.due_at > :today
          AND a.completed_at IS NULL
          AND (a.due_date IS NULL OR a.due_date >= :nowIso)
        """
    )
    suspend fun getNextDueDateActive(userId: Int, today: String, nowIso: String): String?
}

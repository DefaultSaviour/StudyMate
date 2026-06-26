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

    // Look up a single deck by id; null if it no longer exists. Used by the review
    // screen to find the deck's parent assignment (completed decks don't reschedule).
    @Query("SELECT * FROM Flashcard_Decks WHERE id = :deckId")
    suspend fun getDeck(deckId: Int): FlashcardDeck?

    // Get all decks belonging to one assignment. Used by the backup export.
    @Query("SELECT * FROM Flashcard_Decks WHERE assignment_id = :assignmentId")
    suspend fun getDecksForAssignment(assignmentId: Int): List<FlashcardDeck>

    // Get each deck together with all the flashcards inside it for a user.
    @Transaction
    @Query("SELECT * FROM Flashcard_Decks WHERE user_id = :userId")
    suspend fun getDecksWithCards(userId: Int): List<DeckWithCards>

    // Get the earliest due date for cards in each deck, grouped by deck and day.
    // This gives us one review event per deck per day that it has cards due.
    @Query(
        """
        SELECT d.id AS deckId, d.name AS deckName, a.id AS assignmentId, a.color AS assignmentColor, a.icon AS assignmentIcon, c.due_at AS dueAt
        FROM Flashcard_Decks d
        JOIN Flash_Cards c ON d.id = c.deck_id
        JOIN Assignments a ON d.assignment_id = a.id
        WHERE d.user_id = :userId 
          AND c.due_at IS NOT NULL 
          AND c.due_at >= :today
        GROUP BY d.id, c.due_at
        """
    )
    suspend fun getDeckReviewDates(userId: Int, today: String): List<DeckReviewDate>
}

data class DeckReviewDate(
    val deckId: Int,
    val deckName: String,
    val assignmentId: Int,
    val assignmentColor: String?,
    val assignmentIcon: String,
    val dueAt: String
)

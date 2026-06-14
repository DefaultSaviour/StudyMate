package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.ReviewLog
import uws.ac.uk.studymate.util.SpacedRepetition
import java.time.Instant
import java.time.LocalDate

/*//////////////////////
Coded by Jamie Coleman
 12/03/26
 *//////////////////////
// Handles flashcard-related database operations through the DAO.
class CardRepo(private val db: StudyMateDatabase) {

    // Save a new flashcard to the database.
    suspend fun addCard(card: FlashCard) = db.cardDao().insert(card)

    // Update an existing flashcard's content.
    suspend fun updateCard(card: FlashCard) = db.cardDao().update(card)

    // Get all flashcards that belong to a specific deck.
    suspend fun getCards(deckId: Int) = db.cardDao().getCards(deckId)

    // Remove a flashcard from the database.
    suspend fun deleteCard(card: FlashCard) = db.cardDao().delete(card)

    // Cards in a deck that are due for review today (or earlier, or never reviewed).
    suspend fun getDueCardsForDeck(deckId: Int, today: LocalDate = LocalDate.now()) =
        db.cardDao().getDueCardsForDeck(deckId, today.toString())

    // Apply one SM-2 review: roll the card's schedule forward and log the review.
    // A single repo call = one logical review (card update + history row).
    suspend fun reviewCard(card: FlashCard, grade: Int, today: LocalDate = LocalDate.now()) {
        val result = SpacedRepetition.schedule(
            SpacedRepetition.State(card.easeFactor, card.intervalDays, card.repetitions),
            grade,
            today
        )
        val nowIso = Instant.now().toString()
        db.cardDao().update(
            card.copy(
                easeFactor = result.easeFactor,
                intervalDays = result.intervalDays,
                repetitions = result.repetitions,
                dueAt = result.dueDate.toString(),
                lastReviewedAt = nowIso
            )
        )
        db.reviewLogDao().insert(
            ReviewLog(userId = card.userId, cardId = card.id, reviewedAt = nowIso, grade = grade)
        )
    }
}

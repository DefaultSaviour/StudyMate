package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.repositories.CardRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.notifications.ReviewReminderScheduler
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.SpacedRepetition
import java.time.LocalDate
/*//////////////////////
Drives a review session for one deck. Each card is graded Again / Wrong / Correct:

  Correct -> SM-2 schedules it further out (Good) and it leaves the session.
  Wrong   -> SM-2 resets it (lapse, due again tomorrow) and it leaves the session
             too — you've graded it, so it won't reappear this session (it'll come
             back on its next due date).
  Again   -> same lapse scheduling as Wrong, but it goes to the FRONT of the
             queue so you see it again immediately (use it when you want another go
             right now).

For the SM-2 "when is this next due" maths, Again and Wrong are treated the same
(both a lapse). Either way CardRepo.reviewCard logs the review for the stats screen.

When the session opens and again when it finishes, we (re)schedule a single
notification for the day the next cards come due (ReviewReminderScheduler).
 *//////////////////////
class ReviewDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val cardRepo = CardRepo(db)
    private val userRepo = UserRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    enum class Grade { AGAIN, WRONG, CORRECT }

    sealed interface State {
        object Loading : State
        data class Empty(val deckName: String) : State                 // nothing due
        data class Reviewing(
            val deckName: String,
            val card: FlashCard,
            val remaining: Int                                         // cards left this session (incl. current)
        ) : State
        data class Done(val deckName: String, val reviewedCount: Int) : State
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    private var deckId: Int = -1
    private var deckName: String = "Deck"
    private val queue = ArrayDeque<FlashCard>()
    private var doneCount = 0   // distinct cards finished (graded Correct) in the current deck

    // True when the current deck's assignment is finished (marked done or past-due).
    // Reviewing such a deck is read-only practice — grading never re-schedules (SM-2)
    // or logs a review, so a finished deck can't pull its cards back into rotation.
    private var currentDeckCompleted = false

    // Multi-deck chaining (from the dashboard "Review due decks" button): walk each
    // queued deck's due cards, then immediately move on to the next. chainTotal carries
    // the count completed in decks already finished this session.
    private val deckChain = ArrayDeque<Pair<Int, String>>()
    private var isChain = false
    private var chainTotal = 0

    fun load(deckId: Int, deckName: String) {
        isChain = false
        this.deckId = deckId
        this.deckName = deckName
        startQueue(dueOnly = true)
    }

    // Review several decks back-to-back. Decks are walked in the order given.
    fun loadChain(deckIds: List<Int>, deckNames: List<String>) {
        isChain = true
        chainTotal = 0
        deckChain.clear()
        deckIds.forEachIndexed { i, id -> deckChain.addLast(id to (deckNames.getOrNull(i) ?: "Deck")) }
        advanceToNextDeck()
    }

    // Move to the next deck in the chain (if any). Returns false when the chain is empty.
    private fun advanceToNextDeck(): Boolean {
        val next = deckChain.removeFirstOrNull() ?: return false
        deckId = next.first
        deckName = next.second
        startQueue(dueOnly = true)
        return true
    }

    // Fallback offered when nothing is due: run through the whole deck anyway.
    fun reviewAll() = startQueue(dueOnly = false)

    private fun startQueue(dueOnly: Boolean) {
        _state.postValue(State.Loading)
        viewModelScope.launch {
            // Is this deck's assignment finished? If so, grading won't reschedule/log.
            val assignment = db.deckDao().getDeck(deckId)?.let { db.assignmentDao().getById(it.assignmentId) }
            currentDeckCompleted = assignment != null &&
                AssignmentDateTimeUtils.isComplete(assignment.completedAt, assignment.dueDate)

            val cards = if (dueOnly) cardRepo.getDueCardsForDeck(deckId) else cardRepo.getCards(deckId)
            queue.clear()
            queue.addAll(cards)
            doneCount = 0
            // In a chain, a deck that turns out to have nothing due is skipped silently.
            if (isChain && queue.isEmpty() && deckChain.isNotEmpty()) {
                advanceToNextDeck()
                return@launch
            }
            emitCurrent()
            rescheduleReviewReminder()
        }
    }

    fun grade(grade: Grade) {
        val current = (_state.value as? State.Reviewing)?.card ?: return
        viewModelScope.launch {
            // Finished decks are read-only practice: don't touch the SM-2 schedule or
            // log the review. Active decks grade normally (Again/Wrong = lapse, Correct
            // = advance).
            if (!currentDeckCompleted) {
                val quality = if (grade == Grade.CORRECT) SpacedRepetition.GOOD else SpacedRepetition.AGAIN
                cardRepo.reviewCard(current, quality)
            }

            if (queue.isNotEmpty()) queue.removeFirst()
            when (grade) {
                // Both Correct and Wrong are final for this session — the card has
                // been graded and leaves the queue. Only Again brings it straight back.
                Grade.CORRECT -> doneCount++
                Grade.WRONG -> doneCount++
                Grade.AGAIN -> queue.addFirst(current)   // re-show immediately
            }
            if (queue.isEmpty()) {
                // This deck is done. In a chain with more decks ahead, roll its tally
                // into the running total and immediately start the next deck.
                if (isChain && deckChain.isNotEmpty()) {
                    chainTotal += doneCount
                    advanceToNextDeck()
                    return@launch
                }
                // Whole session finished — the cards just reviewed have fresh due
                // dates, so refresh the "cards are due" reminder to match.
                rescheduleReviewReminder()
            }
            emitCurrent()
        }
    }

    private fun emitCurrent() {
        _state.postValue(
            when {
                queue.isEmpty() && doneCount == 0 -> State.Empty(deckName)
                // On the final deck, report the whole chain's total, not just this deck.
                queue.isEmpty() -> State.Done(deckName, if (isChain) chainTotal + doneCount else doneCount)
                else -> State.Reviewing(deckName, queue.first(), queue.size)
            }
        )
    }

    // Schedule the next "your flashcards are due" notification for this user based
    // on the soonest upcoming SM-2 due date. No-op if push notifications are off.
    private suspend fun rescheduleReviewReminder() {
        val session = sessionResolver.requireUser() ?: return
        // Ignore cards under finished/past-due assignments when picking the next reminder.
        val nextDue = db.cardDao().getNextDueDateActive(
            session.userId, LocalDate.now().toString(), java.time.LocalDateTime.now().toString()
        )
        ReviewReminderScheduler.scheduleNextReview(getApplication(), session.value, nextDue)
    }
}

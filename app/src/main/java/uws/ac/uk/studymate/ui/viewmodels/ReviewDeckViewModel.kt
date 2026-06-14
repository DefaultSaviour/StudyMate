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
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.SpacedRepetition
import java.time.LocalDate
/*//////////////////////
Drives a review session for one deck. Each card is graded Again / Wrong / Correct:

  Correct -> SM-2 schedules it further out (Good) and it leaves the session.
  Wrong   -> SM-2 resets it (lapse, due again tomorrow) and it goes to the BACK
             of the session queue — you'll see it again before the session ends.
  Again   -> same lapse scheduling as Wrong, but it goes to the FRONT of the
             queue so you see it again immediately.

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
    private var doneCount = 0   // distinct cards finished (graded Correct)

    fun load(deckId: Int, deckName: String) {
        this.deckId = deckId
        this.deckName = deckName
        startQueue(dueOnly = true)
    }

    // Fallback offered when nothing is due: run through the whole deck anyway.
    fun reviewAll() = startQueue(dueOnly = false)

    private fun startQueue(dueOnly: Boolean) {
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val cards = if (dueOnly) cardRepo.getDueCardsForDeck(deckId) else cardRepo.getCards(deckId)
            queue.clear()
            queue.addAll(cards)
            doneCount = 0
            emitCurrent()
            rescheduleReviewReminder()
        }
    }

    fun grade(grade: Grade) {
        val current = (_state.value as? State.Reviewing)?.card ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Again and Wrong are both a lapse for the SM-2 schedule; Correct advances it.
            val quality = if (grade == Grade.CORRECT) SpacedRepetition.GOOD else SpacedRepetition.AGAIN
            cardRepo.reviewCard(current, quality)

            if (queue.isNotEmpty()) queue.removeFirst()
            when (grade) {
                Grade.CORRECT -> doneCount++
                Grade.WRONG -> queue.addLast(current)    // re-show later this session
                Grade.AGAIN -> queue.addFirst(current)   // re-show immediately
            }
            // Session finished — the cards just reviewed have fresh due dates, so
            // refresh the "cards are due" reminder to match.
            if (queue.isEmpty()) rescheduleReviewReminder()
            emitCurrent()
        }
    }

    private fun emitCurrent() {
        _state.postValue(
            when {
                queue.isEmpty() && doneCount == 0 -> State.Empty(deckName)
                queue.isEmpty() -> State.Done(deckName, doneCount)
                else -> State.Reviewing(deckName, queue.first(), queue.size)
            }
        )
    }

    // Schedule the next "your flashcards are due" notification for this user based
    // on the soonest upcoming SM-2 due date. No-op if push notifications are off.
    private suspend fun rescheduleReviewReminder() {
        val session = sessionResolver.requireUser() ?: return
        val nextDue = db.cardDao().getNextDueDate(session.userId, LocalDate.now().toString())
        ReviewReminderScheduler.scheduleNextReview(getApplication(), session.value, nextDue)
    }
}

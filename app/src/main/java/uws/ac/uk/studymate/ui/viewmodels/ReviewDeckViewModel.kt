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
/*//////////////////////
Drives an SM-2 review session for one deck. Holds the queue of due cards and
walks through it; each grade rolls the card's schedule forward via
CardRepo.reviewCard (which also logs the review for the stats screen).

A card graded "Again" is rescheduled for tomorrow (interval 1) and is NOT
re-shown in the same session — it returns on the next session. (Keeps the MVP
queue simple; intra-session relearning can be added later.)
 *//////////////////////
class ReviewDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepo = CardRepo(StudyMateDatabase.getInstance(application))

    sealed interface State {
        object Loading : State
        data class Empty(val deckName: String) : State                 // nothing due
        data class Reviewing(
            val deckName: String,
            val card: FlashCard,
            val position: Int,   // 1-based index in the session
            val total: Int
        ) : State
        data class Done(val deckName: String, val reviewedCount: Int) : State
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    private var deckId: Int = -1
    private var deckName: String = "Deck"
    private var queue: List<FlashCard> = emptyList()
    private var index = 0
    private var reviewed = 0

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
            queue = if (dueOnly) cardRepo.getDueCardsForDeck(deckId) else cardRepo.getCards(deckId)
            index = 0
            reviewed = 0
            emitCurrent()
        }
    }

    fun grade(grade: Int) {
        val current = (_state.value as? State.Reviewing)?.card ?: return
        viewModelScope.launch(Dispatchers.IO) {
            cardRepo.reviewCard(current, grade)
            reviewed++
            index++
            emitCurrent()
        }
    }

    private fun emitCurrent() {
        _state.postValue(
            when {
                queue.isEmpty() -> State.Empty(deckName)
                index >= queue.size -> State.Done(deckName, reviewed)
                else -> State.Reviewing(deckName, queue[index], index + 1, queue.size)
            }
        )
    }
}

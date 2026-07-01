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
import uws.ac.uk.studymate.util.ExamGenerator
import uws.ac.uk.studymate.util.SessionUserResolver

/*//////////////////////
Drives a Mock Exam session (1.2): multiple-choice questions built from a deck's own
cards, purely for assessment/cramming. Deliberately never calls CardRepo.reviewCard —
no SM-2 reschedule, no Review_Logs row, so an exam can never affect the spaced-
repetition due dates or the streak.

Re-checks the >=8-card minimum itself even though the launching button already gates
on it, mirroring the "worker re-verifies at fire time" defensive pattern used
elsewhere (AssignmentReminderWorker, ReviewReminderWorker) — the deck could have lost
cards between the button being enabled and this screen loading.
 *//////////////////////
class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val cardRepo = CardRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    sealed interface State {
        object Loading : State
        data class Empty(val deckName: String) : State                 // not enough cards
        data class Question(
            val deckName: String,
            val index: Int,                                            // 0-based
            val total: Int,
            val prompt: String,
            val options: List<String>,
            val correctIndex: Int,
            val selectedIndex: Int?,
            val revealed: Boolean
        ) : State
        data class Done(val deckName: String, val correctCount: Int, val total: Int) : State
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private var deckId: Int = -1
    private var deckName: String = "Deck"
    private var cards: List<FlashCard> = emptyList()
    private var questions: List<ExamGenerator.ExamQuestion> = emptyList()
    private var currentIndex = 0
    private var correctCount = 0
    private var selectedIndex: Int? = null
    private var revealed = false

    fun load(deckId: Int, deckName: String) {
        this.deckId = deckId
        this.deckName = deckName
        _state.postValue(State.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            cards = cardRepo.getCards(deckId)
            if (cards.size < ExamGenerator.MIN_CARDS) {
                _state.postValue(State.Empty(deckName))
                return@launch
            }
            startSession()
        }
    }

    fun restart() {
        if (cards.size < ExamGenerator.MIN_CARDS) {
            _state.postValue(State.Empty(deckName))
            return
        }
        startSession()
    }

    private fun startSession() {
        questions = ExamGenerator.generate(cards)
        currentIndex = 0
        correctCount = 0
        selectedIndex = null
        revealed = false
        emitCurrent()
    }

    fun answer(optionIndex: Int) {
        if (revealed) return   // already locked in for this question
        val q = questions.getOrNull(currentIndex) ?: return
        selectedIndex = optionIndex
        revealed = true
        // Grade by TEXT, not index: a degenerate deck (duplicate answer texts) can
        // put the correct text in the options twice via the distractor fallback, and
        // tapping the "other" identical option must still count as correct.
        val chosen = q.options.getOrNull(optionIndex)
        if (chosen != null && chosen == q.options[q.correctIndex]) correctCount++
        emitCurrent()
    }

    fun next() {
        if (!revealed) return   // must answer before advancing
        currentIndex++
        selectedIndex = null
        revealed = false
        if (currentIndex >= questions.size) {
            _state.postValue(State.Done(deckName, correctCount, questions.size))
        } else {
            emitCurrent()
        }
    }

    private fun emitCurrent() {
        val q = questions.getOrNull(currentIndex) ?: return
        _state.postValue(
            State.Question(
                deckName = deckName,
                index = currentIndex,
                total = questions.size,
                prompt = q.prompt,
                options = q.options,
                correctIndex = q.correctIndex,
                selectedIndex = selectedIndex,
                revealed = revealed
            )
        )
    }
}

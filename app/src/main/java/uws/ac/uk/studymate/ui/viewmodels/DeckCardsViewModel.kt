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
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DeckCardsSummary(
    val deckName: String,
    val subjectName: String,
    val cards: List<FlashCard>,
    val dueText: String   // "6 cards due now" / "Next review tomorrow" / "Next review in 3 days" / ""
)

class DeckCardsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val cardRepo = CardRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _summary = MutableLiveData<DeckCardsSummary>()
    val summary: LiveData<DeckCardsSummary> = _summary

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private var deckId: Int = -1
    private var deckName: String = "Deck"

    fun load(deckId: Int, deckName: String) {
        this.deckId = deckId
        this.deckName = deckName
        reload()
    }

    private fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            val cards = cardRepo.getCards(deckId).sortedBy { it.id }
            val deck = db.deckDao().getDecks(session.userId).firstOrNull { it.id == deckId }
            val subjectName = deck?.let {
                db.subjectDao().getSubjects(session.userId).firstOrNull { s -> s.id == it.subjectId }?.name
            } ?: "Unassigned"
            val today = LocalDate.now()
            val dueText = dueTextFor(cards.map { it.dueAt }, today, today.toString())
            _summary.postValue(DeckCardsSummary(deck?.name ?: deckName, subjectName, cards, dueText))
            _sessionExpired.postValue(false)
        }
    }

    fun addCard(front: String, back: String) {
        val cleanFront = TextSanitizer.multiLine(front)
        val cleanBack = TextSanitizer.multiLine(back)
        if (cleanFront.isEmpty()) {
            _message.value = "Enter the front text"
            return
        }
        if (cleanBack.isEmpty()) {
            _message.value = "Enter the back text"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            cardRepo.addCard(
                FlashCard(
                    userId = session.userId,
                    deckId = deckId,
                    front = cleanFront,
                    back = cleanBack
                )
            )
            _message.postValue("Card added")
            reload()
        }
    }

    fun updateCard(original: FlashCard, front: String, back: String) {
        val cleanFront = TextSanitizer.multiLine(front)
        val cleanBack = TextSanitizer.multiLine(back)
        if (cleanFront.isEmpty()) {
            _message.value = "Enter the front text"
            return
        }
        if (cleanBack.isEmpty()) {
            _message.value = "Enter the back text"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            cardRepo.updateCard(original.copy(front = cleanFront, back = cleanBack))
            _message.postValue("Card updated")
            reload()
        }
    }

    fun deleteCard(card: FlashCard) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            cardRepo.deleteCard(card)
            _message.postValue("Card deleted")
            reload()
        }
    }

    // Cards allow multi-line content (questions/answers may span lines), so we
    // only collapse runs of whitespace inside each line and trim. Newlines OK.

    // Deck-screen review summary. If cards are due now, report the count. Otherwise
    // look at the *actual* earliest future due date (SM-2 schedules cards days/weeks
    // out) and word it from that real gap — so a card due in 3 days reads
    // "Next review in 3 days", never falsely "tomorrow".
    private fun dueTextFor(dueDates: List<String?>, today: LocalDate, todayStr: String): String {
        if (dueDates.isEmpty()) return ""
        val dueNow = dueDates.count { it == null || it <= todayStr }
        if (dueNow > 0) {
            return if (dueNow == 1) "1 card due now" else "$dueNow cards due now"
        }
        val nextStr = dueDates.filterNotNull().filter { it > todayStr }.minOrNull() ?: return ""
        val days = ChronoUnit.DAYS.between(today, LocalDate.parse(nextStr))
        return when {
            days <= 1 -> "Next review tomorrow"
            days < 7 -> "Next review in $days days"
            else -> {
                val weeks = days / 7
                if (weeks == 1L) "Next review in 1 week" else "Next review in $weeks weeks"
            }
        }
    }
}

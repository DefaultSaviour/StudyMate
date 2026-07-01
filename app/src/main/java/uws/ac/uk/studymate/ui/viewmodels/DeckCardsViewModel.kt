package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
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
import uws.ac.uk.studymate.util.CsvCardExporter
import uws.ac.uk.studymate.util.CsvCardParser
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DeckCardsSummary(
    val deckName: String,
    val assignmentName: String,
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

    // Fires once per export request: the CSV text to hand to the share sheet, or an
    // empty string when the deck has no cards. The Activity clears it back to null
    // via consumeExportedCsv() so a rotation doesn't re-fire the share intent.
    private val _exportedCsv = MutableLiveData<String?>()
    val exportedCsv: LiveData<String?> = _exportedCsv

    fun consumeExportedCsv() { _exportedCsv.value = null }

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
            val assignmentName = deck?.let {
                db.assignmentDao().getAssignments(session.userId).firstOrNull { a -> a.id == it.assignmentId }?.title
            } ?: "Unassigned"
            val today = LocalDate.now()
            val dueText = dueTextFor(cards.map { it.dueAt }, today, today.toString())
            _summary.postValue(DeckCardsSummary(deck?.name ?: deckName, assignmentName, cards, dueText))
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
            // New cards default to dueAt=null (due now), so this changes the widget's
            // due-card count immediately.
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            reload()
        }
    }

    // Import cards from a CSV/TSV file the user picked (Quizlet/Anki/spreadsheet).
    // Reads the file off the UI thread, then hands the text to the shared importer.
    fun importCsv(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val raw = try {
                resolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
            } catch (_: Exception) {
                null
            }
            if (raw == null) {
                _message.postValue("Couldn't read that file")
                return@launch
            }

            importParsedText(raw, session.userId, emptyMessage = "No cards found in that file")
        }
    }

    // Import cards from text the user pasted from the clipboard (e.g. Quizlet's
    // "Copy text" export). Same parser as the file path — no file needed.
    fun importFromText(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            if (raw.isBlank()) {
                _message.postValue("Nothing on the clipboard to paste")
                return@launch
            }
            importParsedText(raw, session.userId, emptyMessage = "No cards found on the clipboard")
        }
    }

    // Shared CSV/TSV import: parse, append as new cards, toast the outcome, reload.
    private suspend fun importParsedText(raw: String, userId: Int, emptyMessage: String) {
        val result = CsvCardParser.parse(raw)
        if (result.cards.isEmpty()) {
            _message.postValue(emptyMessage)
            return
        }

        cardRepo.addCards(
            result.cards.map { (front, back) ->
                FlashCard(userId = userId, deckId = deckId, front = front, back = back)
            }
        )

        val n = result.cards.size
        val base = "Imported $n card${if (n == 1) "" else "s"}"
        _message.postValue(
            if (result.skipped > 0) "$base · skipped ${result.skipped} bad row${if (result.skipped == 1) "" else "s"}"
            else base
        )
        // Imported cards default to dueAt=null (due now) — refresh the widget's count.
        uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
        reload()
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
            // Front/back text only — doesn't touch due_at, so no widget refresh needed
            // (the widget shows due-card counts, not card content).
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
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            reload()
        }
    }

    // Build this deck's cards into shareable CSV text (1.2 peer deck sharing). Kept
    // through the ViewModel — like every other DB read here — rather than having the
    // Activity call CardRepo directly.
    fun exportDeckCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            val cards = cardRepo.getCards(deckId)
            // Empty string is the "nothing to share" sentinel — CsvCardExporter always
            // emits at least the header row, so an empty deck's CSV is never blank.
            _exportedCsv.postValue(if (cards.isEmpty()) "" else CsvCardExporter.toCsv(cards))
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

package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.DeckRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.LocalDate
/*//////////////////////
Coded by Jamie Coleman
17/04/26
redesigned 18/04/26 — flat list + add/edit panel-swap to match Subjects/Assignments
*//////////////////////
data class DeckListItem(
    val deck: FlashcardDeck,
    val subjectName: String,
    val subjectColorHex: String?,
    val cardCount: Int,
    val dueText: String   // short badge for the list row: "6 due" if any are due now, else ""
)

data class FlashcardDecksSummary(
    val titleText: String,
    val items: List<DeckListItem>,
    val subjects: List<Subject>
)

class FlashcardDecksViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val userRepo = UserRepo(db)
    private val deckRepo = DeckRepo(db)
    private val sessionResolver = SessionUserResolver(application, userRepo)

    private val _screenSummary = MutableLiveData<FlashcardDecksSummary>()
    val screenSummary: LiveData<FlashcardDecksSummary> = _screenSummary

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _createdDeckId = MutableLiveData<Int?>()
    val createdDeckId: LiveData<Int?> = _createdDeckId

    fun loadScreen() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val userId = session.userId
            val subjects = db.subjectDao().getSubjects(userId).sortedBy { it.name.lowercase() }
            val subjectsById = subjects.associateBy { it.id }
            val decksWithCards = db.deckDao().getDecksWithCards(userId)
            val today = LocalDate.now()
            val todayStr = today.toString()

            val items = decksWithCards
                .map { dwc ->
                    val subject = subjectsById[dwc.deck.subjectId]
                    DeckListItem(
                        deck = dwc.deck,
                        subjectName = subject?.name ?: "Unknown subject",
                        subjectColorHex = subject?.color,
                        cardCount = dwc.cards.size,
                        dueText = dueBadgeFor(dwc.cards.map { it.dueAt }, todayStr)
                    )
                }
                .sortedWith(
                    compareBy<DeckListItem> { it.subjectName.lowercase() }
                        .thenBy { it.deck.name.lowercase() }
                )

            _screenSummary.postValue(
                FlashcardDecksSummary(
                    titleText = "Flashcards",
                    items = items,
                    subjects = subjects
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    fun createDeck(name: String, subject: Subject?) {
        val trimmedName = TextSanitizer.singleLine(name)
        if (trimmedName.isEmpty()) {
            _message.value = "Enter a deck name"
            return
        }
        if (subject == null) {
            _message.value = "Choose a subject first"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val newId = deckRepo.addDeck(
                FlashcardDeck(
                    userId = session.userId,
                    subjectId = subject.id,
                    name = trimmedName
                )
            )
            _message.postValue("Deck created")
            _createdDeckId.postValue(newId.toInt())
        }
    }

    fun updateDeck(original: FlashcardDeck, newName: String, subject: Subject?) {
        val trimmedName = TextSanitizer.singleLine(newName)
        if (trimmedName.isEmpty()) {
            _message.value = "Enter a deck name"
            return
        }
        if (subject == null) {
            _message.value = "Choose a subject first"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            deckRepo.updateDeck(original.copy(name = trimmedName, subjectId = subject.id))
            _message.postValue("Deck updated")
            loadScreen()
        }
    }

    fun deleteDeck(deck: FlashcardDeck) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            deckRepo.deleteDeck(deck)
            _message.postValue("Deck deleted")
            loadScreen()
        }
    }

    // Short badge for the deck list row: "6 due" when cards are due now, else "".
    // The fuller "next review …" wording lives inside the deck screen, where there
    // is room for it without truncating the row subtitle.
    private fun dueBadgeFor(dueDates: List<String?>, todayStr: String): String {
        if (dueDates.isEmpty()) return ""
        val dueNow = dueDates.count { it == null || it <= todayStr }
        return if (dueNow > 0) "$dueNow due" else ""
    }

    fun clearCreatedDeckId() {
        _createdDeckId.value = null
    }

}

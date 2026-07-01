package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.repositories.DeckRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import uws.ac.uk.studymate.util.TextSanitizer
import java.time.LocalDate
import java.time.LocalDateTime
/*//////////////////////
Coded by Jamie Coleman
17/04/26
redesigned 18/04/26 — flat list + add/edit panel-swap to match Assignments
merged 17/06/26 — decks now belong to an Assignment (Subject merged away)
*//////////////////////
data class DeckListItem(
    val deck: FlashcardDeck,
    val assignmentName: String,
    val assignmentColorHex: String?,
    val cardCount: Int,
    val dueText: String,  // short badge for the list row: "6 due" if any are due now, else ""
    val isCompleted: Boolean = false  // owning assignment is done / past due → shown in its own subsection
)

data class FlashcardDecksSummary(
    val titleText: String,
    val items: List<DeckListItem>,
    val assignments: List<Assignment>
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

    // When filterAssignmentId is non-null the list is scoped to that one assignment
    // (used when the Flashcards screen is opened from a calendar assignment).
    fun loadScreen(filterAssignmentId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val userId = session.userId
            val assignments = db.assignmentDao().getAssignments(userId).sortedBy { it.title.lowercase() }
            val assignmentsById = assignments.associateBy { it.id }
            val decksWithCards = db.deckDao().getDecksWithCards(userId)
                .filter { filterAssignmentId == null || it.deck.assignmentId == filterAssignmentId }
            val today = LocalDate.now()
            val todayStr = today.toString()
            val now = LocalDateTime.now()

            val items = decksWithCards
                .map { dwc ->
                    val assignment = assignmentsById[dwc.deck.assignmentId]
                    val completed = isAssignmentCompleted(assignment, now)
                    DeckListItem(
                        deck = dwc.deck,
                        assignmentName = assignment?.title ?: "Unknown assignment",
                        assignmentColorHex = assignment?.color,
                        cardCount = dwc.cards.size,
                        // A completed assignment's deck is out of review, so don't show a "due" badge.
                        dueText = if (completed) "" else dueBadgeFor(dwc.cards.map { it.dueAt }, todayStr),
                        isCompleted = completed
                    )
                }
                .sortedWith(
                    // Active decks first, then completed ones; each group by assignment + deck name.
                    compareBy<DeckListItem> { it.isCompleted }
                        .thenBy { it.assignmentName.lowercase() }
                        .thenBy { it.deck.name.lowercase() }
                )

            _screenSummary.postValue(
                FlashcardDecksSummary(
                    titleText = "Flashcards",
                    items = items,
                    assignments = assignments
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    fun createDeck(name: String, assignment: Assignment?) {
        val trimmedName = TextSanitizer.singleLine(name)
        if (trimmedName.isEmpty()) {
            _message.value = "Enter a deck name"
            return
        }
        if (assignment == null) {
            _message.value = "Choose an assignment first"
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
                    assignmentId = assignment.id,
                    name = trimmedName
                )
            )
            _message.postValue("Deck created")
            _createdDeckId.postValue(newId.toInt())
        }
    }

    fun updateDeck(original: FlashcardDeck, newName: String, assignment: Assignment?) {
        val trimmedName = TextSanitizer.singleLine(newName)
        if (trimmedName.isEmpty()) {
            _message.value = "Enter a deck name"
            return
        }
        if (assignment == null) {
            _message.value = "Choose an assignment first"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }
            deckRepo.updateDeck(original.copy(name = trimmedName, assignmentId = assignment.id))
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
            // Deleting a deck cascades its cards — may change the widget's due count.
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            loadScreen()
        }
    }

    // An assignment counts as completed once it's manually marked done OR its due
    // date has passed (same rule as the Statistics screen).
    private fun isAssignmentCompleted(assignment: Assignment?, now: LocalDateTime): Boolean {
        if (assignment == null) return false
        if (assignment.completedAt != null) return true
        val due = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate) ?: return false
        return due.isBefore(now)
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

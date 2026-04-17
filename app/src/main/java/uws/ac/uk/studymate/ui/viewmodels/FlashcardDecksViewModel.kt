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
import uws.ac.uk.studymate.data.repositories.FlashcardDeckRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
/*//////////////////////
Coded by Jamie Coleman
17/04/26
*//////////////////////
// One subject heading with the decks that belong to it.
data class SubjectDecksGroup(
    val subject: Subject,
    val decks: List<FlashcardDeck>
)

// Holds the data the flashcard decks screen needs to display.
data class FlashcardDecksSummary(
    val titleText: String,
    val groups: List<SubjectDecksGroup>,
    val subjects: List<Subject>
)

class FlashcardDecksViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load deck data.
    private val db = StudyMateDatabase.getInstance(application)

    // Use repositories to keep database logic out of the ViewModel.
    private val userRepo = UserRepo(db)
    private val deckRepo = FlashcardDeckRepo(db)

    // Use the session manager so this screen always reads data for the logged-in user.
    private val sessionManager = SessionManager(application)

    // This private value stores the latest decks screen data.
    private val _screenSummary = MutableLiveData<FlashcardDecksSummary>()

    // This public version lets the UI observe the latest decks screen data.
    val screenSummary: LiveData<FlashcardDecksSummary> = _screenSummary

    // This private value stores whether the session is missing or no longer valid.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    // This private value stores the latest validation or save message.
    private val _message = MutableLiveData<String?>()

    // This public version lets the UI show validation and save messages.
    val message: LiveData<String?> = _message

    // This private value stores the ID of a newly created deck so the UI can navigate to it.
    private val _createdDeckId = MutableLiveData<Int?>()

    // This public version lets the UI navigate after a new deck is created.
    val createdDeckId: LiveData<Int?> = _createdDeckId

    fun loadScreen() {
        // Run the database work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {

            // Stop early when there is no logged-in user saved in the session.
            val userId = sessionManager.getLoggedInUserId()
            if (userId == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            // Load the user and end the session if their account no longer exists.
            val user = userRepo.getUser(userId)
            if (user == null) {
                sessionManager.logout()
                _sessionExpired.postValue(true)
                return@launch
            }

            // Load subjects and decks, then group decks under their subject.
            val subjects = db.subjectDao().getSubjects(userId).sortedBy { it.name.lowercase() }
            val decks = deckRepo.getDecks(userId)
            val decksBySubject = decks.groupBy { it.subjectId }

            val groups = subjects
                .filter { decksBySubject.containsKey(it.id) }
                .map { subject ->
                    SubjectDecksGroup(
                        subject = subject,
                        decks = decksBySubject[subject.id]!!.sortedBy { it.name.lowercase() }
                    )
                }

            // Send the finished screen data back to the UI.
            _screenSummary.postValue(
                FlashcardDecksSummary(
                    titleText = "Flashcards",
                    groups = groups,
                    subjects = subjects
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    fun createDeck(name: String, subject: Subject?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _message.value = "Enter a deck name"
            return
        }

        if (subject == null) {
            _message.value = "Choose a subject first"
            return
        }

        // Run the save work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getLoggedInUserId()
            if (userId == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val newId = deckRepo.addDeck(
                FlashcardDeck(
                    userId = userId,
                    subjectId = subject.id,
                    name = trimmedName
                )
            )
            _message.postValue("Deck created")
            _createdDeckId.postValue(newId.toInt())
        }
    }

    // Clear the created deck ID after the UI has navigated.
    fun clearCreatedDeckId() {
        _createdDeckId.value = null
    }
}


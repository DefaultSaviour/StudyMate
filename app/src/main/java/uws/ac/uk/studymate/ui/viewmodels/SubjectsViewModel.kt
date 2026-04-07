package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.relations.SubjectWithAssignments
import uws.ac.uk.studymate.data.repositories.SubjectRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager

// Holds one color option that the add subject form can show by name.
data class SubjectColorChoice(
    val label: String,
    val hex: String
)

// Holds the data that the subjects screen needs to display.
data class SubjectsSummary(
    val titleText: String,
    val subjectsWithAssignments: List<SubjectWithAssignments>,
    val colorChoices: List<SubjectColorChoice>
)

class SubjectsViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load and save subject data.
    private val db = StudyMateDatabase.getInstance(application)

    // Use repositories to keep database logic out of the ViewModel.
    private val userRepo = UserRepo(db)
    private val subjectRepo = SubjectRepo(db)

    // Use the session manager so this screen always reads data for the logged-in user.
    private val sessionManager = SessionManager(application)

    // This private value stores the latest subjects screen data.
    // It is mutable here so only the ViewModel can change it.
    private val _screenSummary = MutableLiveData<SubjectsSummary>()

    // This public version lets the UI observe the latest subjects screen data.
    val screenSummary: LiveData<SubjectsSummary> = _screenSummary

    // This private value stores whether the session is missing or no longer valid.
    // It is mutable here so only the ViewModel can change it.
    private val _sessionExpired = MutableLiveData<Boolean>()

    // This public version lets the UI react when it needs to send the user back to login.
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    // This private value stores the latest validation or save message.
    // It is mutable here so only the ViewModel can change it.
    private val _message = MutableLiveData<String?>()

    // This public version lets the UI show validation and save messages.
    val message: LiveData<String?> = _message

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

            // Load the subjects and their assignments for this screen.
            val subjectsWithAssignments = subjectRepo.getSubjectsWithAssignments(userId)
                .sortedBy { it.subject.name.lowercase() }

            // Send the finished screen data back to the UI.
            _screenSummary.postValue(
                SubjectsSummary(
                    titleText = "Subjects",
                    subjectsWithAssignments = subjectsWithAssignments,
                    colorChoices = buildColorChoices()
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    fun addSubject(name: String, colorChoice: SubjectColorChoice?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _message.value = "Enter a subject name"
            return
        }

        if (colorChoice == null) {
            _message.value = "Choose a subject color"
            return
        }

        // Run the save work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getLoggedInUserId()
            if (userId == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val user = userRepo.getUser(userId)
            if (user == null) {
                sessionManager.logout()
                _sessionExpired.postValue(true)
                return@launch
            }

            // Stop the save when the user already has a subject with the same name.
            val existingSubject = subjectRepo.getSubjectByName(userId, trimmedName)
            if (existingSubject != null) {
                _message.postValue("That subject name is already in use")
                return@launch
            }

            // Save the new subject with the chosen color.
            subjectRepo.addSubject(userId, trimmedName, colorChoice.hex)
            _message.postValue("Subject added")
            loadScreen()
        }
    }

    fun deleteSubject(subject: Subject?) {
        if (subject == null) {
            _message.value = "Choose a subject first"
            return
        }

        // Run the delete work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getLoggedInUserId()
            if (userId == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val user = userRepo.getUser(userId)
            if (user == null) {
                sessionManager.logout()
                _sessionExpired.postValue(true)
                return@launch
            }

            // Remove the chosen subject. Room will also remove its assignments by cascade.
            subjectRepo.deleteSubject(subject)
            _message.postValue("Subject deleted")
            loadScreen()
        }
    }

    // Build the small fixed list of color names that the add subject form can use.
    private fun buildColorChoices(): List<SubjectColorChoice> {
        return listOf(
            SubjectColorChoice(label = "Purple", hex = "#A855F7"),
            SubjectColorChoice(label = "Pink", hex = "#EC4899"),
            SubjectColorChoice(label = "Blue", hex = "#3B82F6"),
            SubjectColorChoice(label = "Yellow", hex = "#EAB308"),
            SubjectColorChoice(label = "Green", hex = "#22C55E"),
            SubjectColorChoice(label = "Orange", hex = "#F97316")
        )
    }
}


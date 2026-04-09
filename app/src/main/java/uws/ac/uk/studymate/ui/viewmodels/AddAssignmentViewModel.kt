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
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.AssignmentRepo
import uws.ac.uk.studymate.data.repositories.SubjectRepo
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentIcons
import uws.ac.uk.studymate.util.SessionManager

// Holds the small set of values that the add assignment screen needs to display.
data class AddAssignmentSummary(
    val titleText: String,
    val subjects: List<Subject>
)

class AddAssignmentViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can load and save assignment data.
    private val db = StudyMateDatabase.getInstance(application)

    // Use repositories to keep database lookup and save logic out of the ViewModel.
    private val userRepo = UserRepo(db)
    private val subjectRepo = SubjectRepo(db)
    private val assignmentRepo = AssignmentRepo(db)

    // Use the session manager so this screen always saves data for the logged-in user.
    private val sessionManager = SessionManager(application)

    // This private value stores the latest screen data.
    // It is mutable here so only the ViewModel can change it.
    private val _screenSummary = MutableLiveData<AddAssignmentSummary>()

    // This public version lets the UI observe the latest screen data.
    val screenSummary: LiveData<AddAssignmentSummary> = _screenSummary

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

    // This private value stores whether the assignment was saved successfully.
    // It is mutable here so only the ViewModel can change it.
    private val _assignmentSaved = MutableLiveData<Boolean>()

    // This public version lets the UI finish the screen after saving.
    val assignmentSaved: LiveData<Boolean> = _assignmentSaved

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

            // Load the subjects that the dropdown needs to show.
            val subjects = subjectRepo.getSubjects(userId).sortedBy { it.name.lowercase() }

            // Send the finished screen data back to the UI.
            _screenSummary.postValue(
                AddAssignmentSummary(
                    titleText = "Add assignment",
                    subjects = subjects
                )
            )
            _message.postValue(null)
            _assignmentSaved.postValue(false)
            _sessionExpired.postValue(false)
        }
    }

    fun saveAssignment(title: String, selectedSubject: Subject?, dueDate: String?, iconKey: String?) {
        // Validate the entered values before trying to save.
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            _message.value = "Enter an assignment title"
            return
        }

        if (selectedSubject == null) {
            _message.value = "Choose a subject first"
            return
        }

        if (dueDate.isNullOrBlank()) {
            _message.value = "Choose a due date"
            return
        }

        val savedIconKey = AssignmentIcons.optionForKey(iconKey).key

        // Run the save work on a background thread.
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

            // Save the new assignment using the entered values.
            assignmentRepo.addAssignment(
                Assignment(
                    userId = userId,
                    subjectId = selectedSubject.id,
                    title = trimmedTitle,
                    dueDate = dueDate,
                    icon = savedIconKey
                )
            )

            // Tell the UI that the save worked so it can return to calendar.
            _message.postValue("Assignment saved")
            _assignmentSaved.postValue(true)
        }
    }
}


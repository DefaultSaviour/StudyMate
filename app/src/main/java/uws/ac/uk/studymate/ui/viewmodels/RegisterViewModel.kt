package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
/*//////////////////////
Coded by Jamie Coleman
15/03/26
fixed 06/04/24
 *//////////////////////
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can work with saved user data.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep database logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the session manager so a new account is logged in as soon as it is created.
    private val sessionManager = SessionManager(application)


    // This private value stores whether registration worked.
    // It is mutable here so only the ViewModel can change it.
    private val _registrationSuccess = MutableLiveData<Boolean>()

    // This public version lets the UI observe the result without changing it.
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    // This private value stores any registration error message.
    // It is mutable here so only the ViewModel can update it.
    private val _errorMessage = MutableLiveData<String?>()

    // This public version lets the UI show the latest error message.
    val errorMessage: LiveData<String?> = _errorMessage


    fun register(name: String, email: String, password: String) {
        // Run the registration work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {

            // Clean up the entered values first so extra spaces do not create bad data.
            val trimmedName = name.trim()
            val trimmedEmail = email.trim()

            // Stop early if any required field is empty.
            if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
                _errorMessage.postValue("Please fill in all fields")
                _registrationSuccess.postValue(false)
                return@launch
            }

            // Check whether another account already uses this email.
            val existingUser = repo.getUserByEmail(trimmedEmail)
            if (existingUser != null) {
                _errorMessage.postValue("An account with that email already exists")
                _registrationSuccess.postValue(false)
                return@launch
            }

            // Create the new user, save their session, and clear any previous error.
            val newUserId = repo.createUserWithDefaults(trimmedName, trimmedEmail, password)
            sessionManager.login(newUserId)
            _errorMessage.postValue(null)

            // Tell the UI that registration finished successfully.
            _registrationSuccess.postValue(true)
        }
    }
}

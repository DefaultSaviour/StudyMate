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

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)

    // The UI observes this to know if registration succeeded
    private val _registrationSuccess = MutableLiveData<Boolean>()
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    // The UI observes this to show an error message when something goes wrong
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {

            // Basic blank field check
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _errorMessage.postValue("Please fill in all fields")
                _registrationSuccess.postValue(false)
                return@launch
            }

            // Check if email is already in use
            val existingUser = repo.getUserByEmail(email)
            if (existingUser != null) {
                _errorMessage.postValue("An account with that email already exists")
                _registrationSuccess.postValue(false)
                return@launch
            }

            // All good — create the user
            repo.createUserWithDefaults(name, email, password)
            _errorMessage.postValue(null)
            _registrationSuccess.postValue(true)
        }
    }
}
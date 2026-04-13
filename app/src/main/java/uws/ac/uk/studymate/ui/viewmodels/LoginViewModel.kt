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
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can check saved user accounts.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep database logic out of the ViewModel.
    private val repo = UserRepo(db)

    // Use the session manager so the app remembers which user is logged in.
    private val sessionManager = SessionManager(application)

    // This private value stores whether login worked.
    // It is mutable here so only the ViewModel can change it.
    private val _loginSuccess = MutableLiveData<Boolean>()

    // This public version lets the UI observe the login result without changing it.
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    // This private value stores any login error message.
    // It is mutable here so only the ViewModel can update it.
    private val _errorMessage = MutableLiveData<String?>()

    // This public version lets the UI show the latest error message.
    val errorMessage: LiveData<String?> = _errorMessage

    // Disabled for now: this testing-only ClearAllData state was used to tell the UI
    // that every table had been wiped. It is commented out so it can be re-enabled later.
//    private val _dataCleared = MutableLiveData<Boolean>()
//
//    // Disabled for now: this testing-only observer value was used by the login screen
//    // after wiping the database. It is commented out so it can be re-enabled later.
//    val dataCleared: LiveData<Boolean> = _dataCleared

    fun login(email: String, password: String) {
        // Run the login work on a background thread.
        viewModelScope.launch(Dispatchers.IO) {

            // Disabled for now: this testing-only reset was only needed for the
            // ClearAllData feature, which is currently commented out.
//            _dataCleared.postValue(false)

            // Clean up the email first so leading or trailing spaces do not break login.
            // This should have been part of the 'FormattingUtil'
            val trimmedEmail = email.trim()

            // Stop early if the user has not filled in every field.
            if (trimmedEmail.isBlank() || password.isBlank()) {
                _errorMessage.postValue("Please enter your email and password")
                _loginSuccess.postValue(false)
                return@launch
            }

            // Check whether the entered email and password match a saved account.
            val user = repo.authenticateUser(trimmedEmail, password)
            if (user == null) {
                _errorMessage.postValue("Incorrect email or password")
                _loginSuccess.postValue(false)
                return@launch
            }

            // Save the logged-in user ID so the rest of the app knows who is active.
            sessionManager.login(user.id)
            _errorMessage.postValue(null)

            // Tell the UI that login finished successfully.
            _loginSuccess.postValue(true)
        }
    }

    // Disabled for now: this testing-only ClearAllData helper wiped every Room table.
    // It is commented out so it stays in the file and can be re-enabled later.
//    fun clearAllData() {
//        // Run the reset work on a background thread because Room does not allow it on the UI thread.
//        viewModelScope.launch(Dispatchers.IO) {
//
//            // Remove the saved session first so no old user stays logged in after the wipe.
//            sessionManager.logout()
//
//            // Clear every Room table so the app starts again with empty data.
//            db.clearAllTables()
//
//            // Tell the UI the reset finished and clear any older error message.
//            _errorMessage.postValue(null)
//            _loginSuccess.postValue(false)
//            _dataCleared.postValue(true)
//        }
//    }
}


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
Multi-user sign-in.

  signInWithPassword(username, password)
  signInWithBiometricCreds(email, password)  — plaintext creds from the
                                                encrypted store, still verified
                                                through PBKDF2 the same way.
 *//////////////////////
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)
    private val sessionManager = SessionManager(application)

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun signInWithPassword(username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = username.trim()
            if (cleanName.isBlank() || password.isBlank()) {
                _errorMessage.postValue("Enter your username and password")
                _loginSuccess.postValue(false)
                return@launch
            }
            val user = repo.getUserByName(cleanName)
            if (user == null) {
                _errorMessage.postValue("No account with that username")
                _loginSuccess.postValue(false)
                return@launch
            }
            if (user.authMode == SessionManager.AUTH_MODE_BIOMETRIC_ONLY) {
                _errorMessage.postValue("This account signs in with fingerprint or screen lock")
                _loginSuccess.postValue(false)
                return@launch
            }
            val authed = repo.authenticateUser(user.email, password)
            if (authed == null) {
                _errorMessage.postValue("Incorrect password")
                _loginSuccess.postValue(false)
                return@launch
            }
            sessionManager.login(authed.id)
            sessionManager.setLastUserId(authed.id)
            // The widget shows the logged-in user's data — refresh now rather than
            // leaving the previous (or "not logged in") state up for ~30min.
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            _errorMessage.postValue(null)
            _loginSuccess.postValue(true)
        }
    }

    fun signInWithBiometricCreds(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val authed = repo.authenticateUser(email, password)
            if (authed == null) {
                _errorMessage.postValue("Saved credentials no longer match an account")
                _loginSuccess.postValue(false)
                return@launch
            }
            sessionManager.login(authed.id)
            sessionManager.setLastUserId(authed.id)
            // The widget shows the logged-in user's data — refresh now rather than
            // leaving the previous (or "not logged in") state up for ~30min.
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            _errorMessage.postValue(null)
            _loginSuccess.postValue(true)
        }
    }
}

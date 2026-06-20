package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.SampleContentSeeder
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionManager
import uws.ac.uk.studymate.util.TextSanitizer
import java.security.SecureRandom
import java.util.UUID

/*//////////////////////
Multi-user signup with the one-bio rule:
  - createPasswordUser(name, pw) : adds a new account with a typed password.
  - createBiometricUser(name)    : adds the device's biometric-only account.
                                   Blocked if one already exists.
Usernames are unique (case-insensitive).
 *//////////////////////
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)
    private val sampleSeeder = SampleContentSeeder(db)
    private val sessionManager = SessionManager(application)

    private val _registrationSuccess = MutableLiveData<Boolean>()
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    data class BiometricCredentials(
        val userId: Int,
        val email: String,
        val password: String
    )

    private val _biometricCredentials = MutableLiveData<BiometricCredentials?>()
    val biometricCredentials: LiveData<BiometricCredentials?> = _biometricCredentials

    fun consumeBiometricCredentials() { _biometricCredentials.value = null }

    fun createPasswordUser(name: String, password: String, confirmPassword: String) {
        val cleanName = TextSanitizer.singleLine(name)
        if (cleanName.isBlank()) {
            _errorMessage.value = "Please enter a username"
            _registrationSuccess.value = false
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            _registrationSuccess.value = false
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Passwords don't match"
            _registrationSuccess.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (repo.getUserByName(cleanName) != null) {
                _errorMessage.postValue("That username is already taken")
                _registrationSuccess.postValue(false)
                return@launch
            }

            val newUserId = repo.createUserWithDefaults(
                name = cleanName,
                email = placeholderEmail(),
                password = password,
                authMode = "password"
            )
            sessionManager.login(newUserId)
            sessionManager.setLastUserId(newUserId)
            // Pre-load the first-run sample deck so the new account isn't empty (0.9E).
            sampleSeeder.seed(newUserId)
            _errorMessage.postValue(null)
            _registrationSuccess.postValue(true)
        }
    }

    fun createBiometricUser(name: String) {
        val cleanName = TextSanitizer.singleLine(name)
        if (cleanName.isBlank()) {
            _errorMessage.value = "Please enter a username"
            _registrationSuccess.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (repo.getUserByName(cleanName) != null) {
                _errorMessage.postValue("That username is already taken")
                _registrationSuccess.postValue(false)
                return@launch
            }
            if (repo.getBiometricOnlyUser() != null) {
                _errorMessage.postValue("Another account on this device already uses fingerprint/screen-lock sign-in")
                _registrationSuccess.postValue(false)
                return@launch
            }

            val email = placeholderEmail()
            val password = generateRandomPassword()
            val newUserId = repo.createUserWithDefaults(
                name = cleanName,
                email = email,
                password = password,
                authMode = SessionManager.AUTH_MODE_BIOMETRIC_ONLY
            )
            sessionManager.login(newUserId)
            sessionManager.setLastUserId(newUserId)
            // Pre-load the first-run sample deck so the new account isn't empty (0.9E).
            sampleSeeder.seed(newUserId)
            _errorMessage.postValue(null)
            _biometricCredentials.postValue(BiometricCredentials(newUserId, email, password))
            _registrationSuccess.postValue(true)
        }
    }

    // Email column still exists in the schema — give every multi-user account a
    // unique placeholder so no two collide if the column ever gains constraints.
    private fun placeholderEmail(): String =
        "local-${UUID.randomUUID().toString().take(12)}@studymate.local"

    private fun generateRandomPassword(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

}

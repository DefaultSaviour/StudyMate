package uws.ac.uk.studymate.util

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/*//////////////////////
Biometric quick-login for StudyMate.

Stores the user's email + password in EncryptedSharedPreferences (AES-256 with a
Keystore-backed master key) and gates retrieval behind a BiometricPrompt.

On a successful biometric, we hand the plaintext credentials back to the normal
LoginViewModel.login() path so authentication still runs through PBKDF2 the same
way it does for manual sign-in. The stored credentials are wiped on logout,
password change, account deletion, or the user toggling biometric login off.
 *//////////////////////
class BiometricLoginManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    enum class Availability {
        AVAILABLE,
        NO_HARDWARE,
        HW_UNAVAILABLE,
        NONE_ENROLLED,
        UNSUPPORTED
    }

    fun availability(): Availability {
        val bm = BiometricManager.from(context)
        return when (bm.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else -> Availability.UNSUPPORTED
        }
    }

    fun isReadyToAuthenticate(): Boolean = availability() == Availability.AVAILABLE

    /** True if the user has *any* screen lock (PIN/pattern/password/biometric) set. */
    fun hasDeviceLock(): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false) && hasStoredCredentials()

    fun hasStoredCredentials(): Boolean =
        prefs.contains(KEY_EMAIL) && prefs.contains(KEY_PASSWORD)

    fun storedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** The id of the user this device's biometric slot belongs to (or -1 if none). */
    fun storedUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun saveCredentials(userId: Int, email: String, password: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    data class Credentials(val email: String, val password: String)

    /**
     * Show the biometric/device-credential prompt; on success invoke [onSuccess]
     * with the stored credentials. [onError] receives a human-readable message
     * for non-success outcomes (user cancel, no credentials, lockout, etc.);
     * pass null to ignore those cases.
     */
    fun promptForLogin(
        activity: FragmentActivity,
        title: String = "Sign in to StudyMate",
        subtitle: String = "Use your fingerprint, face, or screen lock",
        onSuccess: (Credentials) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (!hasStoredCredentials()) {
            onError?.invoke("No saved credentials")
            return
        }
        runPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onAuthSuccess = {
                val email = prefs.getString(KEY_EMAIL, null)
                val password = prefs.getString(KEY_PASSWORD, null)
                if (email == null || password == null) {
                    onError?.invoke("No saved credentials")
                } else {
                    onSuccess(Credentials(email, password))
                }
            },
            onError = onError
        )
    }

    /**
     * Show the prompt purely to verify the device owner — no stored credentials
     * are returned. Used by Quick Start sign-up to gate account creation behind
     * the phone's lock screen.
     */
    fun promptForDeviceVerification(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        runPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onAuthSuccess = onSuccess,
            onError = onError
        )
    }

    private fun runPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onAuthSuccess: () -> Unit,
        onError: ((String) -> Unit)?
    ) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError?.invoke("")
                    } else {
                        onError?.invoke(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Attempt not recognised — BiometricPrompt keeps the dialog
                    // open for another try, so nothing to do here.
                }
            }
        )

        // Note: setNegativeButtonText is not allowed when DEVICE_CREDENTIAL is
        // in the allowed authenticators — the system supplies the cancel control.
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }

    companion object {
        private const val PREFS_NAME = "studymate_biometric"
        private const val KEY_ENABLED = "biometric_enabled"
        private const val KEY_USER_ID = "biometric_user_id"
        private const val KEY_EMAIL = "biometric_email"
        private const val KEY_PASSWORD = "biometric_password"

        // BIOMETRIC_STRONG + DEVICE_CREDENTIAL is supported on API 30+; we
        // target minSdk 30 so this combination is always valid.
        // Means the prompt accepts fingerprint, face (strong only), pattern,
        // PIN, or password as authentication.
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

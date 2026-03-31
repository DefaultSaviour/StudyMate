package uws.ac.uk.studymate.util



import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordUtils {

    // Creates a random salt - this makes each hash unique even if two users
    // have the same password
    fun generateSalt(): String {
        val salt = ByteArray(16) // creates a 16 byte array
        SecureRandom().nextBytes(salt) // fills the array with random bytes
        return Base64.encodeToString(salt, Base64.NO_WRAP) // convert to string
    }

    // Hashes the password combined with the salt using SHA-256
    fun hashPassword(password: String, salt: String): String {
        val input = salt + password // combine salt and password
        val digest = MessageDigest.getInstance("SHA-256") // this is the hash algorithm
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8)) // hash the input
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP) // convert to string

    }

    // Checks if a password the user typed matches the stored hash
    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val hashOfAttempt = hashPassword(password, salt) // hash the input password
        return hashOfAttempt == storedHash // compare the hashes

    }
}
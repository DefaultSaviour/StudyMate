package uws.ac.uk.studymate.util



import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordUtils {

    // Create a random salt so matching passwords do not produce the same hash.
    fun generateSalt(): String {
        val salt = ByteArray(16) // Create a 16-byte array for the random salt.
        SecureRandom().nextBytes(salt) // Fill the array with random bytes.
        return Base64.encodeToString(salt, Base64.NO_WRAP) // Convert the salt into a storable string.
    }

    // Hash the password together with its salt using SHA-256.
    fun hashPassword(password: String, salt: String): String {
        val input = salt + password // Combine the salt and password into one value.
        val digest = MessageDigest.getInstance("SHA-256") // Create the SHA-256 hasher.
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8)) // Hash the combined value.
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP) // Convert the hash into a storable string.

    }

    // Check whether the entered password matches the saved hash.
    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val hashOfAttempt = hashPassword(password, salt) // Hash the entered password with the saved salt.
        return hashOfAttempt == storedHash // Compare the new hash with the saved hash.

    }
}
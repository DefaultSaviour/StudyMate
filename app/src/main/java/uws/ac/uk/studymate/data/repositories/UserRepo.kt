package uws.ac.uk.studymate.data.repositories

import androidx.room.Transaction
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.entities.UserStats
import uws.ac.uk.studymate.util.PasswordUtils

class UserRepo(private val db: StudyMateDatabase) {

    @Transaction
    suspend fun createUserWithDefaults(name: String, email: String, password: String) {

        // Generate salt and hash the password before saving
        val salt = PasswordUtils.generateSalt()
        val hash = PasswordUtils.hashPassword(password, salt)

        val userId = db.userDao().insert(
            User(
                name = name,
                email = email,
                passwordHash = hash,   // store the hash, not the plain password
                passwordSalt = salt    // store the salt so we can verify later
            )
        )

        db.userSettingsDao().insert(
            UserSettings(
                userId = userId.toInt(),
                notificationsEnabled = true,
                darkModeEnabled = false,
                timezone = "UTC"
            )
        )

        db.userStatsDao().insert(
            UserStats(
                userId = userId.toInt(),
                assignmentsCount = 0,
                flashcardsCount = 0, // thats buissness logic
                streakDays = 0
            )
        )
    }

    suspend fun getUserByEmail(email: String): User? {
        return db.userDao().getByEmail(email)
    }

    suspend fun updateSettings(userId: Int, notifications: Boolean, darkMode: Boolean) {
        db.userSettingsDao().update(
            UserSettings(
                userId = userId,
                notificationsEnabled = notifications,
                darkModeEnabled = darkMode
            )
        )
    }

    suspend fun deleteUser(user: User) {
        db.userDao().delete(user)
    }

    suspend fun getUser(id: Int) = db.userDao().getById(id)

    ////////////////////// for testing only //////////////////////
    suspend fun getAllUsers() = db.userDao().getAll()
////////////////////// for testing only //////////////////////

}
package uws.ac.uk.studymate.data.repositories

import androidx.room.Transaction
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.entities.UserStats
import uws.ac.uk.studymate.data.relations.UserWithSettingsAndStats
import uws.ac.uk.studymate.util.PasswordUtils

// Handles user-related database operations through the DAOs.
class UserRepo(private val db: StudyMateDatabase) {
    /*//////////////////////
    Coded by Jamie Coleman
     11/03/26
      updated 7/04/26
     *//////////////////////
    // Create a new user together with default settings and stats in one transaction.
    // Return the new user ID so the app can log that user in straight away - this
    // is a workaround im sure this isnt the idiomatic was of doing, it but it works.
    @Transaction
    suspend fun createUserWithDefaults(name: String, email: String, password: String): Int {

        // Generate a random salt and hash the password before saving.
        val salt = PasswordUtils.generateSalt()
        val hash = PasswordUtils.hashPassword(password, salt)

        // Save the user and get back their generated ID.
        val userId = db.userDao().insert(
            User(
                name = name,
                email = email,
                passwordHash = hash,   // Store the hash, not the plain password.
                passwordSalt = salt    // Store the salt so we can verify later.
            )
        )

        // Create default settings for the new user.
        db.userSettingsDao().insert(
            UserSettings(
                userId = userId.toInt(),
                notificationsEnabled = true,
                darkModeEnabled = false,
                timezone = "UTC"
            )
        )

        // Create empty stats for the new user.
        db.userStatsDao().insert(
            UserStats(
                userId = userId.toInt(),
                assignmentsCount = 0,
                flashcardsCount = 0,
                streakDays = 0
            )
        )

        return userId.toInt()
    }

    // Find a user by their email address, or return null if not found.
    suspend fun getUserByEmail(email: String): User? {
        return db.userDao().getByEmail(email)
    }

    // Check whether the email and password match a saved account.
    // Return the user when the login details are correct, or null when they are not.
    suspend fun authenticateUser(email: String, password: String): User? {
        val user = db.userDao().getByEmail(email) ?: return null
        val passwordMatches = PasswordUtils.verifyPassword(
            password = password,
            salt = user.passwordSalt,
            storedHash = user.passwordHash
        )

        return if (passwordMatches) user else null
    }

    // Update a user's notification and dark mode preferences.
    suspend fun updateSettings(userId: Int, notifications: Boolean, darkMode: Boolean) {
        db.userSettingsDao().update(
            UserSettings(
                userId = userId,
                notificationsEnabled = notifications,
                darkModeEnabled = darkMode
            )
        )
    }

    // Remove a user from the database.
    suspend fun deleteUser(user: User) {
        db.userDao().delete(user)
    }

    // Find a user by their ID, or return null if they do not exist.
    suspend fun getUser(id: Int) = db.userDao().getById(id)

    // Load a user together with their settings and stats in one call.
    suspend fun getUserWithMeta(id: Int): UserWithSettingsAndStats? {
        return db.userDao().getUserWithMeta(id)
    }

    // Get every user in the database (used for testing only).
    suspend fun getAllUsers() = db.userDao().getAll()

}
package uws.ac.uk.studymate.data.repositories

import androidx.room.Transaction
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.entities.UserStats

class UserRepo(private val db: StudyMateDatabase) {

    @Transaction
    suspend fun createUserWithDefaults(name: String, email: String, password: String) {

        val userId = db.userDao().insert(
            User(
                name = name,
                email = email,
                passwordHash = password
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
                flashcardsCount = 0, // this is gonna need to be a sum of the cards.. is that me or buissness logic??
                streakDays = 0
            )
        )
    }

    suspend fun updateSettings(userId: Int, notifications: Boolean, darkMode: Boolean) {
        db.userSettingsDao().update(
            UserSettings(
                userId = userId,
                notificationsEnabled = if (notifications) true else false,
                darkModeEnabled = if (darkMode) true else false
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

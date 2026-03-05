package uws.ac.uk.studymate.data.repositories

import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings

class UserRepo(private val db: StudyMateDatabase) {

    suspend fun addUser(name: String, email: String, password: String) {
        db.userDao().insert(
            User(
                name = name,
                email = email,
                passwordHash = password
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

package uws.ac.uk.studymate.util

import android.content.Context
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.relations.UserWithSettingsAndStats
import uws.ac.uk.studymate.data.repositories.UserRepo

data class ActiveSession<T>(
    val userId: Int,
    val value: T
)

class SessionUserResolver(
    context: Context,
    private val userRepo: UserRepo
) {

    private val sessionManager = SessionManager(context)

    suspend fun requireUser(): ActiveSession<User>? {
        val userId = sessionManager.getLoggedInUserId() ?: return null
        val user = userRepo.getUser(userId) ?: run {
            sessionManager.logout()
            return null
        }
        return ActiveSession(userId, user)
    }

    suspend fun requireUserWithMeta(): ActiveSession<UserWithSettingsAndStats>? {
        val userId = sessionManager.getLoggedInUserId() ?: return null
        val userWithMeta = userRepo.getUserWithMeta(userId) ?: run {
            sessionManager.logout()
            return null
        }
        return ActiveSession(userId, userWithMeta)
    }

    fun logout() {
        sessionManager.logout()
    }
}


package uws.ac.uk.studymate.util

import android.content.Context

class SessionManager(context: Context) {

    // Use one shared preferences file to keep the current login session.
    // this was the way the tutorial told me how do to it.
    //shared preferences is a way to store data in a key value pair
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Save the logged-in user's ID so the rest of the app knows who is active.
    fun login(userId: Int) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    // Read the current user's ID, or return null when nobody is logged in.
    fun getLoggedInUserId(): Int? {
        val userId = prefs.getInt(KEY_USER_ID, NO_USER_ID)
        return if (userId == NO_USER_ID) null else userId
    }

    // Return true when there is an active logged-in user.
    fun isLoggedIn(): Boolean {
        return getLoggedInUserId() != null
    }

    // Clear the saved session when the user logs out.
    // this was the recommneded code for this function in the tutorial
    fun logout() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .apply()
    }

    companion object {
        // Keep the preference file name in one place so every screen uses the same session.
        // im not 100% sure this works the way i want it too, and im not sure if it is a good idea
        // if this cause issue we need ot look into removing it and replacing it with
        // a unique name for each user, their ID, or something like that
        private const val PREFS_NAME = "studymate_session"

        // Store the active user ID under one stable key.
        // a stable key is a constant value  that is private
        private const val KEY_USER_ID = "logged_in_user_id"

        // Use -1 as the default value when no user is logged in.
        // ROOM shouldn't ever return a user with an ID of -1.
        private const val NO_USER_ID = -1
    }
}


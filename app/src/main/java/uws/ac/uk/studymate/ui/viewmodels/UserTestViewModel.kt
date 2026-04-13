package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.repositories.UserRepo
/*//////////////////////
Coded by Jamie Coleman
10/03/26
- i dont remeber when i removed this
 *//////////////////////
class UserTestViewModel(application: Application) : AndroidViewModel(application) {

    // Get the app database once so this ViewModel can work with saved user data.
    private val db = StudyMateDatabase.getInstance(application)

    // Use the repository to keep database logic out of the ViewModel.
    private val repo = UserRepo(db)

    // This private value stores the latest list of users.
    // It is mutable here so only the ViewModel can update it.
    private val _users = MutableLiveData<List<User>>()

    // This public version lets the UI observe the user list.
    val users: LiveData<List<User>> = _users

    // Create a new user, then reload the list so the UI shows the change.
    fun addUser(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.createUserWithDefaults(name, email, password)
            loadUsers()
        }
    }

    // Update a user's settings, then reload the list with the new values.
    fun updateSettings(id: Int, notifications: Boolean, darkMode: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateSettings(id, notifications, darkMode)
            loadUsers()
        }
    }

    // Find the user by ID, delete them if they exist, then refresh the list.
    fun deleteUser(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repo.getUser(id)
            if (user != null) repo.deleteUser(user)
            loadUsers()
        }
    }

    // Load all users from the database and post the result to the UI.
    fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.getAllUsers()
            _users.postValue(list)
        }
    }
}
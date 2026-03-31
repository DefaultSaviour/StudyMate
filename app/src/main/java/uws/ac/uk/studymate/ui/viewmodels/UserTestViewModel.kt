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

class UserTestViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    // Now calls createUserWithDefaults which handles hashing and salting
    fun addUser(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.createUserWithDefaults(name, email, password)
            loadUsers()
        }
    }

    fun updateSettings(id: Int, notifications: Boolean, darkMode: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateSettings(id, notifications, darkMode)
            loadUsers()
        }
    }

    fun deleteUser(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repo.getUser(id)
            if (user != null) repo.deleteUser(user)
            loadUsers()
        }
    }

    fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.getAllUsers()
            _users.postValue(list)
        }
    }
}
package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.repositories.AssignmentRepo

class AssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AssignmentRepo(
        StudyMateDatabase.getInstance(application)
    )

    // Expose LiveData for observing in Activities
    private val _assignments = MutableLiveData<List<Assignment>>()
    val assignments: LiveData<List<Assignment>> = _assignments

    fun loadAssignments(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: replace temp userId with logged-in user from SessionManager when available
            val list = repo.getAssignments(userId)
            _assignments.postValue(list)
        }
    }

    fun addAssignment(userId: Int, subjectId: Int, title: String, dueDate: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: move validation rules into business logic layer / ViewModel checks if needed
            // TODO: replace temp subjectId mapping with real selected subject from database
            val newAssignment = Assignment(
                userId = userId,
                subjectId = subjectId,
                title = title,
                dueDate = dueDate
            )

            // Add to repo / DB
            repo.addAssignment(newAssignment)

            // TODO: review whether this should refresh via reload or a direct LiveData update later
            loadAssignments(userId)
        }
    }
}
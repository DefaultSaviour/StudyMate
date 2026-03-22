package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

    val assignments = MutableLiveData<List<Assignment>>()

    fun loadAssignments(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            assignments.postValue(repo.getAssignments(userId))
        }
    }

    fun addAssignment(userId: Int, subjectId: Int, title: String, dueDate: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addAssignment(
                Assignment(
                    userId = userId,
                    subjectId = subjectId,
                    title = title,
                    dueDate = dueDate
                )
            )
            loadAssignments(userId)
        }
    }
}
package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.repositories.SubjectRepo

class SubjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SubjectRepo(
        StudyMateDatabase.getInstance(application)
    )

    val subjects = MutableLiveData<List<Subject>>()

    fun loadSubjects(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            subjects.postValue(repo.getSubjects(userId))
        }
    }

    fun addSubject(userId: Int, name: String, color: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addSubject(userId, name, color)
            loadSubjects(userId)
        }
    }

    fun deleteSubject(userId: Int, subject: Subject) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteSubject(subject)
            loadSubjects(userId)
        }
    }
}
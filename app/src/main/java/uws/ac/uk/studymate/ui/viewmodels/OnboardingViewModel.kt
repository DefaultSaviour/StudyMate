package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.SampleContentSeeder
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.Instant

/*//////////////////////
First-run onboarding (0.9E). Resolves the sample deck that was seeded for the
new account (see SampleContentSeeder) so the welcome screen can launch a guided
review of it. Read-only — the deck already exists by the time we get here.
 *//////////////////////
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)
    private val sessionResolver = SessionUserResolver(application, repo)

    // The completion write is fired as the activity finishes onboarding, so it must
    // outlive viewModelScope (which is cancelled in onCleared). A detached one-shot
    // scope keeps the tiny DB write alive across the navigation away.
    private val completionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class SampleDeck(val id: Int, val name: String)

    // null once resolved means "no sample deck found" (the welcome screen then just
    // sends the user to Home rather than into a review).
    private val _sampleDeck = MutableLiveData<SampleDeck?>()
    val sampleDeck: LiveData<SampleDeck?> = _sampleDeck

    fun loadSampleDeck() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sampleDeck.postValue(null)
                return@launch
            }
            val assignment = db.assignmentDao()
                .getByName(session.userId, SampleContentSeeder.ASSIGNMENT_TITLE)
            val deck = assignment?.let { a ->
                val decks = db.deckDao().getDecksForAssignment(a.id)
                decks.firstOrNull { it.name == SampleContentSeeder.DECK_NAME } ?: decks.firstOrNull()
            }
            _sampleDeck.postValue(deck?.let { SampleDeck(it.id, it.name) })
        }
    }

    // Once the user has been through onboarding, the sample "Getting Started"
    // assignment has served its purpose — mark it complete so it reads as done in the
    // Assignments list (deletable, dimmed) and drops out of the review surfaces, rather
    // than lingering until its +1h due date silently passes. No-op if already done.
    fun completeSampleAssignment() {
        completionScope.launch {
            val session = sessionResolver.requireUser() ?: return@launch
            val assignment = db.assignmentDao()
                .getByName(session.userId, SampleContentSeeder.ASSIGNMENT_TITLE) ?: return@launch
            if (assignment.completedAt == null) {
                db.assignmentDao().setCompleted(assignment.id, Instant.now().toString())
                uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            }
        }
    }
}

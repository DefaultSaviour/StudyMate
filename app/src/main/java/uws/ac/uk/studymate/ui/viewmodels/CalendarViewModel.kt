package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.repositories.UserRepo
import uws.ac.uk.studymate.util.AssignmentDateTimeUtils
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class EventType { ASSIGNMENT, DECK_REVIEW, CUSTOM }

// Holds one event that should appear inside a calendar day cell.
data class CalendarEvent(
    val id: Int,
    val type: EventType,
    val title: String,
    val date: LocalDate,
    val timeText: String?, // Pre-formatted time or subtitle (e.g. "14:00" or "All day")
    val colorHex: String?,
    val iconKey: String
)

// Holds the data that the calendar screen needs to display.
data class CalendarSummary(
    val titleText: String,
    val entriesByDate: Map<LocalDate, List<CalendarEvent>>
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyMateDatabase.getInstance(application)
    private val repo = UserRepo(db)
    private val sessionResolver = SessionUserResolver(application, repo)

    private val _calendarSummary = MutableLiveData<CalendarSummary>()
    val calendarSummary: LiveData<CalendarSummary> = _calendarSummary

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    fun loadCalendar() {
        viewModelScope.launch {
            val session = sessionResolver.requireUser()
            if (session == null) {
                _sessionExpired.postValue(true)
                return@launch
            }

            val userId = session.userId
            val user = session.value
            val todayStr = LocalDate.now().toString()

            val allEvents = mutableListOf<CalendarEvent>()

            // 1. Load Assignments
            val assignments = db.assignmentDao().getAssignments(userId)
            assignments.forEach { assignment ->
                val dueAt = AssignmentDateTimeUtils.parseDueDate(assignment.dueDate)
                if (dueAt != null) {
                    allEvents.add(
                        CalendarEvent(
                            id = assignment.id,
                            type = EventType.ASSIGNMENT,
                            title = assignment.title,
                            date = dueAt.toLocalDate(),
                            timeText = AssignmentDateTimeUtils.formatDueTime(dueAt),
                            colorHex = assignment.color,
                            iconKey = assignment.icon
                        )
                    )
                }
            }

            // 2. Load Deck Reviews
            val deckReviews = db.deckDao().getDeckReviewDates(userId, todayStr)
            deckReviews.forEach { review ->
                val reviewDate = try {
                    LocalDate.parse(review.dueAt)
                } catch (e: Exception) {
                    null
                }
                if (reviewDate != null) {
                    allEvents.add(
                        CalendarEvent(
                            id = review.deckId,
                            type = EventType.DECK_REVIEW,
                            title = "Review: ${review.deckName}",
                            date = reviewDate,
                            timeText = "Due", // Simple label for deck reviews
                            colorHex = review.assignmentColor,
                            iconKey = review.assignmentIcon
                        )
                    )
                }
            }

            // 3. Load Custom Events
            val customEvents = db.customEventDao().getEventsForUser(userId)
            customEvents.forEach { event ->
                val eventDate = try {
                    LocalDate.parse(event.date)
                } catch (e: Exception) {
                    null
                }
                if (eventDate != null) {
                    allEvents.add(
                        CalendarEvent(
                            id = event.id,
                            type = EventType.CUSTOM,
                            title = event.title,
                            date = eventDate,
                            timeText = event.time ?: "All day",
                            colorHex = event.color,
                            iconKey = event.icon
                        )
                    )
                }
            }

            // Sort and group
            val entriesByDate = allEvents
                .sortedWith(
                    compareBy<CalendarEvent> { it.date }
                        .thenBy { it.type.ordinal }
                        .thenBy { it.title.lowercase() }
                )
                .groupBy { it.date }

            _calendarSummary.postValue(
                CalendarSummary(
                    titleText = "Calendar for ${user.name}",
                    entriesByDate = entriesByDate
                )
            )
            _sessionExpired.postValue(false)
        }
    }

    fun addCustomEvent(title: String, date: LocalDate, time: String?, remindDayBefore: Boolean, colorHex: String?, iconKey: String = "event") {
        viewModelScope.launch {
            val session = sessionResolver.requireUser() ?: return@launch
            val newEvent = uws.ac.uk.studymate.data.entities.CustomEvent(
                userId = session.userId,
                title = title,
                date = date.toString(),
                time = time,
                remindDayBefore = remindDayBefore,
                color = colorHex,
                icon = iconKey
            )
            db.customEventDao().insert(newEvent)
            
            // Re-fetch the event to get its auto-generated ID so we can schedule it.
            // Since we don't return the ID from insert(), we could just schedule them all,
            // but for simplicity, we'll schedule when we re-load. Wait, actually, let's 
            // schedule it inside the activity or here? It's easier to schedule all CustomEvents 
            // periodically, but let's just let the Activity handle scheduling if we pass the CustomEvent.
            // Actually, we can just loadCalendar() and the activity can update UI.
            // Scheduling will happen in the ViewModel.
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            loadCalendar() // Refresh UI
        }
    }

    fun updateCustomEvent(event: uws.ac.uk.studymate.data.entities.CustomEvent) {
        viewModelScope.launch {
            db.customEventDao().update(event)
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            loadCalendar()
        }
    }

    fun deleteCustomEvent(event: uws.ac.uk.studymate.data.entities.CustomEvent) {
        viewModelScope.launch {
            db.customEventDao().delete(event)
            uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(getApplication())
            loadCalendar()
        }
    }

    suspend fun getCustomEventById(id: Int): uws.ac.uk.studymate.data.entities.CustomEvent? {
        return db.customEventDao().getById(id)
    }
}

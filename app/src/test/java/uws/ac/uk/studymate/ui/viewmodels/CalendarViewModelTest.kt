package uws.ac.uk.studymate.ui.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.dao.AssignmentDao
import uws.ac.uk.studymate.data.dao.CustomEventDao
import uws.ac.uk.studymate.data.dao.DeckReviewDate
import uws.ac.uk.studymate.data.dao.FlashcardDeckDao
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.CustomEvent
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.util.ActiveSession
import uws.ac.uk.studymate.util.MainCoroutineRule
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: CalendarViewModel
    private val application = mockk<Application>(relaxed = true)

    private val database = mockk<StudyMateDatabase>(relaxed = true)
    private val assignmentDao = mockk<AssignmentDao>(relaxed = true)
    private val deckDao = mockk<FlashcardDeckDao>(relaxed = true)
    private val customEventDao = mockk<CustomEventDao>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(StudyMateDatabase.Companion)
        every { StudyMateDatabase.getInstance(any()) } returns database

        every { database.assignmentDao() } returns assignmentDao
        every { database.deckDao() } returns deckDao
        every { database.customEventDao() } returns customEventDao

        mockkConstructor(SessionUserResolver::class)
        coEvery { anyConstructed<SessionUserResolver>().requireUser() } returns ActiveSession(
            userId = 1,
            value = User(id = 1, name = "CalUser", email = "test@test.com", passwordHash = "hash", passwordSalt = "salt")
        )

        viewModel = CalendarViewModel(application)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `loadCalendar groups assignments, decks, and custom events by date`() = runTest {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val assignment = Assignment(
            id = 1, userId = 1, title = "Exam", color = "#000",
            dueDate = tomorrow.atTime(10, 0).toString(), completedAt = null
        )
        coEvery { assignmentDao.getAssignments(1) } returns listOf(assignment)

        val deckReview = DeckReviewDate(
            deckId = 2, deckName = "Vocab", assignmentId = 1, assignmentColor = "#111", assignmentIcon = "icon",
            dueAt = today.toString()
        )
        coEvery { deckDao.getDeckReviewDates(1, any<String>()) } returns listOf(deckReview)

        val customEvent = CustomEvent(
            id = 3, userId = 1, title = "Study Group", date = tomorrow.toString(),
            time = "14:00", remindDayBefore = false, color = null, icon = "event"
        )
        coEvery { customEventDao.getEventsForUser(1) } returns listOf(customEvent)

        viewModel.loadCalendar()
        advanceUntilIdle()

        val summary = viewModel.calendarSummary.value
        assertEquals("Calendar for CalUser", summary?.titleText)
        
        val entries = summary?.entriesByDate
        assertTrue(entries != null)
        assertEquals(2, entries?.size) // Today and Tomorrow

        val todayEvents = entries?.get(today)
        assertEquals(1, todayEvents?.size)
        assertEquals(EventType.DECK_REVIEW, todayEvents?.get(0)?.type)

        val tomorrowEvents = entries?.get(tomorrow)
        assertEquals(2, tomorrowEvents?.size)
        assertEquals(EventType.ASSIGNMENT, tomorrowEvents?.get(0)?.type)
        assertEquals(EventType.CUSTOM, tomorrowEvents?.get(1)?.type)
    }

    @Test
    fun `addCustomEvent saves to DB and refreshes calendar`() = runTest {
        mockkObject(uws.ac.uk.studymate.widget.WidgetUpdater)
        every { uws.ac.uk.studymate.widget.WidgetUpdater.updateAllWidgets(any()) } returns Unit

        coEvery { assignmentDao.getAssignments(any<Int>()) } returns emptyList()
        coEvery { deckDao.getDeckReviewDates(any<Int>(), any<String>()) } returns emptyList()
        
        val today = LocalDate.now()
        val customEvent = CustomEvent(
            id = 1, userId = 1, title = "New Event", date = today.toString(),
            time = "10:00", remindDayBefore = false, color = null, icon = "event"
        )
        
        coEvery { customEventDao.getEventsForUser(1) } returns listOf(customEvent)

        viewModel.addCustomEvent("New Event", today, "10:00", false, null)
        advanceUntilIdle()

        val entries = viewModel.calendarSummary.value?.entriesByDate
        assertEquals(1, entries?.size)
        assertEquals("New Event", entries?.get(today)?.first()?.title)
    }
}

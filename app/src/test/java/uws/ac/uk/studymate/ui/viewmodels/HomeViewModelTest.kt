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
import uws.ac.uk.studymate.data.dao.FlashCardDao
import uws.ac.uk.studymate.data.dao.FlashcardDeckDao
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.util.ActiveSession
import uws.ac.uk.studymate.util.MainCoroutineRule
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: HomeViewModel
    private val application = mockk<Application>(relaxed = true)

    private val database = mockk<StudyMateDatabase>(relaxed = true)
    private val cardDao = mockk<FlashCardDao>(relaxed = true)
    private val deckDao = mockk<FlashcardDeckDao>(relaxed = true)
    private val assignmentDao = mockk<AssignmentDao>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(StudyMateDatabase.Companion)
        every { StudyMateDatabase.getInstance(any()) } returns database

        every { database.cardDao() } returns cardDao
        every { database.deckDao() } returns deckDao
        every { database.assignmentDao() } returns assignmentDao

        mockkConstructor(SessionUserResolver::class)
        coEvery { anyConstructed<SessionUserResolver>().requireUser() } returns ActiveSession(
            userId = 1,
            value = User(id = 1, name = "TestUser", email = "test@test.com", passwordHash = "hash", passwordSalt = "salt")
        )

        viewModel = HomeViewModel(application)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `loadHome with no assignments shows no due assignments`() = runTest {
        coEvery { assignmentDao.getAssignments(1) } returns emptyList()
        coEvery { cardDao.getDueCardsActive(any<Int>(), any<String>(), any<String>()) } returns emptyList()
        coEvery { deckDao.getDecks(1) } returns emptyList()

        viewModel.loadHome()
        advanceUntilIdle()

        val summary = viewModel.homeSummary.value
        assertEquals("Welcome back, TestUser", summary?.welcomeText)
        assertEquals("No due assignments yet", summary?.nextDueCountdown)
        assertTrue(summary?.dueDeckIds?.isEmpty() == true)
        assertEquals(0, summary?.dueCardCount)
    }

    @Test
    fun `loadHome formats next due assignment correctly`() = runTest {
        val tomorrow = LocalDateTime.now().plusDays(1).plusHours(2)
        val assignment = Assignment(
            id = 1,
            userId = 1,
            title = "Math Homework",
            color = "#FF0000",
            dueDate = tomorrow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
            completedAt = null
        )

        coEvery { assignmentDao.getAssignments(1) } returns listOf(assignment)
        coEvery { cardDao.getDueCardsActive(any<Int>(), any<String>(), any<String>()) } returns emptyList()
        coEvery { deckDao.getDecks(1) } returns emptyList()

        viewModel.loadHome()
        advanceUntilIdle()

        val summary = viewModel.homeSummary.value
        assertEquals("Due in 1 day 1 hour", summary?.nextDueCountdown)
        assertTrue(summary?.nextDueDetails?.contains("Math Homework") == true)
    }

    @Test
    fun `loadHome collects due decks and cards accurately`() = runTest {
        coEvery { assignmentDao.getAssignments(1) } returns emptyList()

        val deck1 = FlashcardDeck(id = 10, userId = 1, assignmentId = 1, name = "Deck 1")
        val deck2 = FlashcardDeck(id = 20, userId = 1, assignmentId = 1, name = "Deck 2")
        
        coEvery { deckDao.getDecks(1) } returns listOf(deck1, deck2)
        
        val dueCards = listOf(
            FlashCard(id = 100, userId = 1, deckId = 10, front = "Q", back = "A"),
            FlashCard(id = 101, userId = 1, deckId = 10, front = "Q", back = "A"),
            FlashCard(id = 200, userId = 1, deckId = 20, front = "Q", back = "A")
        )
        coEvery { cardDao.getDueCardsActive(any<Int>(), any<String>(), any<String>()) } returns dueCards

        viewModel.loadHome()
        advanceUntilIdle()

        val summary = viewModel.homeSummary.value
        assertEquals(3, summary?.dueCardCount)
        assertEquals(listOf(10, 20), summary?.dueDeckIds)
        assertEquals(listOf("Deck 1", "Deck 2"), summary?.dueDeckNames)
    }
}

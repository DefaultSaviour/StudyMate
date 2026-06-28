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
import uws.ac.uk.studymate.notifications.ReviewReminderScheduler
import uws.ac.uk.studymate.util.ActiveSession
import uws.ac.uk.studymate.util.MainCoroutineRule
import uws.ac.uk.studymate.util.SessionUserResolver
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewDeckViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: ReviewDeckViewModel
    private val application = mockk<Application>(relaxed = true)
    
    private val database = mockk<StudyMateDatabase>(relaxed = true)
    private val cardDao = mockk<FlashCardDao>(relaxed = true)
    private val deckDao = mockk<FlashcardDeckDao>(relaxed = true)
    private val assignmentDao = mockk<AssignmentDao>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(StudyMateDatabase.Companion)
        every { StudyMateDatabase.getInstance(any()) } returns database
        
        mockkObject(ReviewReminderScheduler)
        every { ReviewReminderScheduler.scheduleNextReview(any(), any(), any()) } returns Unit

        every { database.cardDao() } returns cardDao
        every { database.deckDao() } returns deckDao
        every { database.assignmentDao() } returns assignmentDao

        mockkConstructor(SessionUserResolver::class)
        coEvery { anyConstructed<SessionUserResolver>().requireUser() } returns ActiveSession(
            userId = 1,
            value = User(id = 1, name = "Test", email = "test@test.com", passwordHash = "hash", passwordSalt = "salt")
        )
        val assignment = Assignment(id = 10, userId = 1, title = "A", color = "#000000", dueDate = LocalDate.now().plusDays(1).toString(), completedAt = null)
        val deck = FlashcardDeck(id = 1, userId = 1, assignmentId = 10, name = "Deck")
        
        coEvery { deckDao.getDeck(1) } returns deck
        coEvery { assignmentDao.getById(10) } returns assignment
        
        viewModel = ReviewDeckViewModel(application)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `load deck with no due cards emits Empty state`() = runTest {
        coEvery { cardDao.getDueCardsForDeck(1, any<String>()) } returns emptyList()
        
        viewModel.load(1, "Empty Deck")
        advanceUntilIdle()
        
        val state = viewModel.state.value as ReviewDeckViewModel.State.Empty
        assertEquals("Empty Deck", state.deckName)
    }

    @Test
    fun `load deck with due cards emits Reviewing state`() = runTest {
        val cards = listOf(
            FlashCard(id = 1, userId = 1, deckId = 1, front = "Q1", back = "A1"),
            FlashCard(id = 2, userId = 1, deckId = 1, front = "Q2", back = "A2")
        )
        coEvery { cardDao.getDueCardsForDeck(1, any<String>()) } returns cards
        
        viewModel.load(1, "Test Deck")
        advanceUntilIdle()
        
        val state = viewModel.state.value as ReviewDeckViewModel.State.Reviewing
        assertEquals("Test Deck", state.deckName)
        assertEquals(2, state.remaining)
        assertEquals("Q1", state.card.front)
    }

    @Test
    fun `grade CORRECT advances to next card and removes from queue`() = runTest {
        val cards = listOf(
            FlashCard(id = 1, userId = 1, deckId = 1, front = "Q1", back = "A1"),
            FlashCard(id = 2, userId = 1, deckId = 1, front = "Q2", back = "A2")
        )
        coEvery { cardDao.getDueCardsForDeck(1, any<String>()) } returns cards
        
        viewModel.load(1, "Test Deck")
        advanceUntilIdle()
        
        viewModel.grade(ReviewDeckViewModel.Grade.CORRECT)
        advanceUntilIdle()
        
        val state = viewModel.state.value as ReviewDeckViewModel.State.Reviewing
        assertEquals("Q2", state.card.front)
        assertEquals(1, state.remaining)
    }

    @Test
    fun `grade AGAIN re-adds card to front of queue`() = runTest {
        val cards = listOf(
            FlashCard(id = 1, userId = 1, deckId = 1, front = "Q1", back = "A1"),
            FlashCard(id = 2, userId = 1, deckId = 1, front = "Q2", back = "A2")
        )
        coEvery { cardDao.getDueCardsForDeck(1, any<String>()) } returns cards
        
        viewModel.load(1, "Test Deck")
        advanceUntilIdle()
        
        viewModel.grade(ReviewDeckViewModel.Grade.AGAIN)
        advanceUntilIdle()
        
        val state = viewModel.state.value as ReviewDeckViewModel.State.Reviewing
        assertEquals("Q1", state.card.front)
        assertEquals(2, state.remaining)
    }

    @Test
    fun `finishing queue emits Done state with tally`() = runTest {
        val cards = listOf(FlashCard(id = 1, userId = 1, deckId = 1, front = "Q1", back = "A1"))
        coEvery { cardDao.getDueCardsForDeck(1, any<String>()) } returns cards
        
        viewModel.load(1, "Test Deck")
        advanceUntilIdle()
        
        viewModel.grade(ReviewDeckViewModel.Grade.CORRECT)
        advanceUntilIdle()
        
        val state = viewModel.state.value as ReviewDeckViewModel.State.Done
        assertEquals("Test Deck", state.deckName)
        assertEquals(1, state.reviewedCount)
    }
}

package uws.ac.uk.studymate.data.testutil

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.ReviewLog
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.entities.UserStats
/*//////////////////////
Coded by Jamie Coleman
 02/04/26
  updated 16/04/26 - added push notifications tests
 *//////////////////////
abstract class RoomDbTestBase {

    protected lateinit var db: StudyMateDatabase

    // The User table now has a unique index on `name` (v8). Tests routinely
    // insert several users distinguished only by email, so give each one a
    // distinct default name to avoid spurious unique-constraint failures.
    private var userNameCounter = 0

    @Before
    fun setUpDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, StudyMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDatabase() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    protected suspend fun insertUser(
        name: String? = null,
        email: String = "test@example.com",
        passwordHash: String = "hash",
        passwordSalt: String = "salt",
        authMode: String = "password",
        pushNotificationsEnabled: Boolean? = null
    ): Int {
        return db.userDao().insert(
            User(
                name = name ?: "Test User ${userNameCounter++}",
                email = email,
                passwordHash = passwordHash,
                passwordSalt = passwordSalt,
                authMode = authMode,
                pushNotificationsEnabled = pushNotificationsEnabled
            )
        ).toInt()
    }

    protected suspend fun insertSettings(
        userId: Int,
        darkModeEnabled: Boolean = false,
        timezone: String = "UTC"
    ) {
        db.userSettingsDao().insert(
            UserSettings(
                userId = userId,
                darkModeEnabled = darkModeEnabled,
                timezone = timezone
            )
        )
    }

    protected suspend fun insertStats(
        userId: Int,
        assignmentsCount: Int = 0,
        flashcardsCount: Int = 0,
        streakDays: Int = 0
    ) {
        db.userStatsDao().insert(
            UserStats(
                userId = userId,
                assignmentsCount = assignmentsCount,
                flashcardsCount = flashcardsCount,
                streakDays = streakDays
            )
        )
    }

    // Subject was merged into Assignment (v11): an assignment is now the single
    // top-level study item, carrying its own colour, and decks hang off it.
    protected suspend fun insertAssignment(
        userId: Int,
        title: String = "Essay",
        color: String? = "#FF0000",
        dueDate: String = "2026-05-01T09:00",
        icon: String = "calculator",
        completedAt: String? = null
    ): Int {
        return db.assignmentDao().insert(
            Assignment(
                userId = userId,
                title = title,
                color = color,
                dueDate = dueDate,
                icon = icon,
                completedAt = completedAt
            )
        ).toInt()
    }

    protected suspend fun insertDeck(
        userId: Int,
        assignmentId: Int,
        name: String = "Week 1"
    ): Int {
        return db.deckDao().insert(
            FlashcardDeck(
                userId = userId,
                assignmentId = assignmentId,
                name = name
            )
        ).toInt()
    }

    @Suppress("unused")
    protected suspend fun insertCard(
        userId: Int,
        deckId: Int?,
        front: String = "Question",
        back: String = "Answer",
        dueAt: String? = null,
        easeFactor: Double = 2.5,
        intervalDays: Int = 0,
        repetitions: Int = 0,
        lastReviewedAt: String? = null
    ) {
        db.cardDao().insert(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = front,
                back = back,
                easeFactor = easeFactor,
                intervalDays = intervalDays,
                repetitions = repetitions,
                dueAt = dueAt,
                lastReviewedAt = lastReviewedAt
            )
        )
    }

    protected suspend fun insertReviewLog(
        userId: Int,
        cardId: Int?,
        reviewedAt: String,
        grade: Int = 2
    ) {
        db.reviewLogDao().insert(
            ReviewLog(userId = userId, cardId = cardId, reviewedAt = reviewedAt, grade = grade)
        )
    }

}


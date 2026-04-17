package uws.ac.uk.studymate.data.testutil

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.Assignment
import uws.ac.uk.studymate.data.entities.FlashCard
import uws.ac.uk.studymate.data.entities.FlashcardDeck
import uws.ac.uk.studymate.data.entities.Subject
import uws.ac.uk.studymate.data.entities.SubjectProgress
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
        name: String = "Test User",
        email: String = "test@example.com",
        passwordHash: String = "hash",
        passwordSalt: String = "salt",
        pushNotificationsEnabled: Boolean? = null
    ): Int {
        return db.userDao().insert(
            User(
                name = name,
                email = email,
                passwordHash = passwordHash,
                passwordSalt = passwordSalt,
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

    protected suspend fun insertSubject(
        userId: Int,
        name: String = "Maths",
        color: String? = "#FF0000"
    ): Int {
        return db.subjectDao().insert(
            Subject(
                userId = userId,
                name = name,
                color = color
            )
        ).toInt()
    }

    protected suspend fun insertAssignment(
        userId: Int,
        subjectId: Int,
        title: String = "Essay",
        dueDate: String = "2026-05-01T09:00",
        icon: String = "calculator"
    ) {
        db.assignmentDao().insert(
            Assignment(
                userId = userId,
                subjectId = subjectId,
                title = title,
                dueDate = dueDate,
                icon = icon
            )
        )
    }

    protected suspend fun insertDeck(
        userId: Int,
        subjectId: Int,
        name: String = "Week 1"
    ): Int {
        return db.deckDao().insert(
            FlashcardDeck(
                userId = userId,
                subjectId = subjectId,
                name = name
            )
        ).toInt()
    }

    @Suppress("unused")
    protected suspend fun insertCard(
        userId: Int,
        deckId: Int?,
        front: String = "Question",
        back: String = "Answer"
    ) {
        db.cardDao().insert(
            FlashCard(
                userId = userId,
                deckId = deckId,
                front = front,
                back = back
            )
        )
    }

    protected suspend fun insertProgress(
        userId: Int,
        subjectId: Int,
        completedTasks: Int = 1,
        totalTasks: Int = 3
    ) {
        db.subjectProgressDao().insert(
            SubjectProgress(
                userId = userId,
                subjectId = subjectId,
                completedTasks = completedTasks,
                totalTasks = totalTasks
            )
        )
    }
}


package uws.ac.uk.studymate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uws.ac.uk.studymate.data.StudyMateDatabase
import uws.ac.uk.studymate.data.entities.*
import uws.ac.uk.studymate.data.repositories.SampleContentSeeder
import uws.ac.uk.studymate.ui.*
import uws.ac.uk.studymate.util.SessionManager
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    private lateinit var context: Context
    private lateinit var db: StudyMateDatabase
    private var userId: Int = 1

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = StudyMateDatabase.getInstance(context)

        var user = db.userDao().getByEmail("demo@studymate.app")
        if (user == null) {
            userId = db.userDao().insert(
                User(
                    name = "Alex",
                    email = "demo@studymate.app",
                    passwordHash = "hash",
                    passwordSalt = "salt",
                    authMode = "password",
                    pushNotificationsEnabled = true
                )
            ).toInt()
        } else {
            userId = user.id
        }

        // Seed rich data
        SampleContentSeeder(db).seed(userId)

        // Seed demo assignment and deck
        val aId = db.assignmentDao().insert(
            Assignment(
                userId = userId,
                title = "Computer Science Honours",
                color = "#C4A24A",
                dueDate = "2026-09-01T12:00",
                icon = "code"
            )
        ).toInt()

        val deckId = db.deckDao().insert(
            FlashcardDeck(
                userId = userId,
                assignmentId = aId,
                name = "Algorithms & Data Structures"
            )
        ).toInt()

        db.cardDao().insert(FlashCard(userId = userId, deckId = deckId, front = "What is Big-O complexity of Binary Search?", back = "O(log n) time complexity."))
        db.cardDao().insert(FlashCard(userId = userId, deckId = deckId, front = "Explain Polymorphism in OOP", back = "The ability of different classes to respond to the same method call with distinct implementations."))
        db.cardDao().insert(FlashCard(userId = userId, deckId = deckId, front = "What is an Inverted Index?", back = "A database index storing a mapping from words to their locations in a document collection."))

        // Set session
        SessionManager(context).login(userId)
    }

    private fun saveActivityScreenshot(scenario: ActivityScenario<*>, filename: String) {
        Thread.sleep(1500) // allow entrance animation to settle
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            val w = if (view.width > 0) view.width else 1080
            val h = if (view.height > 0) view.height else 1920
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            val dir = File("/sdcard/Download")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    @Test
    fun captureAllScreenshots() {
        // 1. Home Dashboard
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            saveActivityScreenshot(scenario, "screenshot_1_home.png")
        }

        // 2. Focus Timer
        ActivityScenario.launch(FocusTimerActivity::class.java).use { scenario ->
            saveActivityScreenshot(scenario, "screenshot_2_focustimer.png")
        }

        // 3. Flashcards Decks
        ActivityScenario.launch(FlashcardDecksActivity::class.java).use { scenario ->
            saveActivityScreenshot(scenario, "screenshot_3_flashcards.png")
        }

        // 4. Calendar
        ActivityScenario.launch(CalendarActivity::class.java).use { scenario ->
            saveActivityScreenshot(scenario, "screenshot_4_calendar.png")
        }

        // 5. Trophy Room
        ActivityScenario.launch(TrophyRoomActivity::class.java).use { scenario ->
            saveActivityScreenshot(scenario, "screenshot_5_trophies.png")
        }

        // 6. Assignments
        ActivityScenario.launch(AssignmentsActivity::class.java).use { scenario ->
            saveActivityScreenshot(scenario, "screenshot_6_assignments.png")
        }
    }
}

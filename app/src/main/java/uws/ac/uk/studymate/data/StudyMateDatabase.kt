package uws.ac.uk.studymate.data

import android.content.Context
import androidx.room.*
import uws.ac.uk.studymate.data.dao.*
import uws.ac.uk.studymate.data.entities.*
/*//////////////////////
Coded by Jamie Coleman
 09/03/26
 - i should be tracking migrations as updates
 *//////////////////////

// List every table the database uses and set the current version number.
@Database(
    entities = [
        User::class,
        UserSettings::class,
        UserStats::class,
        Subject::class,
        SubjectProgress::class,
        Assignment::class,
        FlashcardDeck::class,
        FlashCard::class
    ],
    exportSchema = false,
    version = 4
)
abstract class StudyMateDatabase : RoomDatabase() {

    // Give the rest of the app access to each DAO.
    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun subjectDao(): SubjectDao
    abstract fun subjectProgressDao(): SubjectProgressDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun deckDao(): FlashcardDeckDao
    abstract fun cardDao(): FlashCardDao

    companion object {
        // Keep one shared instance so the database is not opened more than once.
        @Volatile
        private var INSTANCE: StudyMateDatabase? = null

        // Return the existing database instance, or create a new one if it does not exist yet.
        fun getInstance(context: Context): StudyMateDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "StudyMate.db"
                )
                    // Clear the old local database so the new assignment icon setup starts clean.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

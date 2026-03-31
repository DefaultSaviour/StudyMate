package uws.ac.uk.studymate.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import uws.ac.uk.studymate.data.dao.*
import uws.ac.uk.studymate.data.entities.*


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
    version = 2
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

        // This migration updates older version-1 databases by adding the password_salt column.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE User ADD COLUMN password_salt TEXT NOT NULL DEFAULT ''")
            }
        }

        // Return the existing database instance, or create a new one if it does not exist yet.
        fun getInstance(context: Context): StudyMateDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "StudyMate.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

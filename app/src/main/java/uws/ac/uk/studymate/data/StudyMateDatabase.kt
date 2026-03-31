package uws.ac.uk.studymate.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import uws.ac.uk.studymate.data.dao.*
import uws.ac.uk.studymate.data.entities.*

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
    exportSchema = false, // Turn this on later if you want Room to save schema history files.
    version = 2
)
abstract class StudyMateDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun subjectDao(): SubjectDao
    abstract fun subjectProgressDao(): SubjectProgressDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun deckDao(): FlashcardDeckDao
    abstract fun cardDao(): FlashCardDao

    companion object {
        @Volatile
        private var INSTANCE: StudyMateDatabase? = null

        // This updates older version 1 databases by adding the new password_salt column.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE User ADD COLUMN password_salt TEXT NOT NULL DEFAULT ''")
            }
        }

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


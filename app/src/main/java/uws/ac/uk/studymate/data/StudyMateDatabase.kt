package uws.ac.uk.studymate.data

import android.content.Context
import androidx.room.*
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
    exportSchema = true,
    version = 1

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
        @Volatile private var INSTANCE: StudyMateDatabase? = null

        fun getInstance(context: Context): StudyMateDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "StudyMate.db"
                ).build().also { INSTANCE = it }
            }
    }
}


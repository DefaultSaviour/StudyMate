package uws.ac.uk.studymate.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.*
import uws.ac.uk.studymate.data.dao.*
import uws.ac.uk.studymate.data.entities.*
/*//////////////////////
Coded by Jamie Coleman
 09/03/26
 - i should be tracking migrations as updates
  updated 16/04/26 - added push notifctaions
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
    version = 5
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
        // Move the notification choice onto the user row and keep the other settings in User_Settings.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `User` ADD COLUMN `push_notifications_enabled` INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `User_Settings_new` (
                        `user_id` INTEGER NOT NULL,
                        `dark_mode_enabled` INTEGER NOT NULL DEFAULT 0,
                        `timezone` TEXT NOT NULL DEFAULT 'UTC',
                        PRIMARY KEY(`user_id`),
                        FOREIGN KEY(`user_id`) REFERENCES `User`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `User_Settings_new` (`user_id`, `dark_mode_enabled`, `timezone`)
                    SELECT `user_id`, `dark_mode_enabled`, `timezone`
                    FROM `User_Settings`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `User_Settings`")
                db.execSQL("ALTER TABLE `User_Settings_new` RENAME TO `User_Settings`")
            }
        }

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
                    // Keep old saved data and move it to the new schema version.
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

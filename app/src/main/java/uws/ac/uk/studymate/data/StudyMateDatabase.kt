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
        FlashCard::class,
        ReviewLog::class
    ],
    exportSchema = false,
    version = 10
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
    abstract fun reviewLogDao(): ReviewLogDao

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

        // Add the account creation timestamp column required by the current User entity.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `User` ADD COLUMN `created_at` TEXT")
                db.execSQL("UPDATE `User` SET `created_at` = CURRENT_TIMESTAMP WHERE `created_at` IS NULL")
            }
        }

        // Icon set replaced with 30 subject icons — wipe all user data so no
        // assignments are left with stale icon keys that no longer exist.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `User`")
            }
        }

        // Switch to the multi-user / one-bio auth model:
        //   - drop the email unique index (email is now an internal placeholder)
        //   - add the auth_mode column (password | biometric_only)
        //   - add a unique index on name (the user-facing identifier)
        // Existing users are wiped first to avoid name collisions on the new
        // index and because the old accounts predate the new model anyway.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `User`")
                db.execSQL("DROP INDEX IF EXISTS `index_User_email`")
                db.execSQL(
                    "ALTER TABLE `User` ADD COLUMN `auth_mode` TEXT NOT NULL DEFAULT 'password'"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_User_name` ON `User` (`name`)")
            }
        }

        // Spaced repetition + assignment completion + review history:
        //   - add SM-2 scheduling columns to Flash_Cards
        //   - add completed_at to Assignments
        //   - create the Review_Logs table (+ its indices)
        // Only additive (new columns / new table), so existing rows are preserved.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Flash_Cards` ADD COLUMN `ease_factor` REAL NOT NULL DEFAULT 2.5")
                db.execSQL("ALTER TABLE `Flash_Cards` ADD COLUMN `interval_days` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `Flash_Cards` ADD COLUMN `repetitions` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `Flash_Cards` ADD COLUMN `due_at` TEXT")
                db.execSQL("ALTER TABLE `Flash_Cards` ADD COLUMN `last_reviewed_at` TEXT")

                db.execSQL("ALTER TABLE `Assignments` ADD COLUMN `completed_at` TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `Review_Logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL,
                        `card_id` INTEGER,
                        `reviewed_at` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        FOREIGN KEY(`user_id`) REFERENCES `User`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`card_id`) REFERENCES `Flash_Cards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Review_Logs_user_id` ON `Review_Logs` (`user_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Review_Logs_card_id` ON `Review_Logs` (`card_id`)")
            }
        }

        // Auto-login toggle — add auto_login_enabled to User, on by default.
        // Additive only; existing accounts inherit the default (auto-login on).
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `User` ADD COLUMN `auto_login_enabled` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)

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
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

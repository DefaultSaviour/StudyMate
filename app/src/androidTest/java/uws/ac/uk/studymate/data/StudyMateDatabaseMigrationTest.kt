package uws.ac.uk.studymate.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyMateDatabaseMigrationTest {

    @Test
    fun migrationFromVersion4To6_addsAllCurrentUserColumns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-${System.currentTimeMillis()}.db"

        val helper = createHelper(dbName)
        helper.writableDatabase.use { db ->
            createVersion4Schema(db)
            seedVersion4User(db)
            db.version = 4
        }

        helper.writableDatabase.use { db ->
            StudyMateDatabase.MIGRATIONS.forEach { migration ->
                migration.migrate(db)
            }
            db.version = 6

            val userColumns = readColumnNames(db, "User")
            assertTrue(userColumns.contains("id"))
            assertTrue(userColumns.contains("name"))
            assertTrue(userColumns.contains("email"))
            assertTrue(userColumns.contains("password_hash"))
            assertTrue(userColumns.contains("password_salt"))
            assertTrue(userColumns.contains("push_notifications_enabled"))
            assertTrue(userColumns.contains("created_at"))

            db.query("SELECT email, push_notifications_enabled, created_at FROM `User` WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("migration@example.com", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.getString(2).isNotBlank())
            }

            val settingsColumns = readColumnNames(db, "User_Settings")
            assertEquals(listOf("user_id", "dark_mode_enabled", "timezone"), settingsColumns)
        }

        context.getDatabasePath(dbName).delete()
    }

    private fun createHelper(dbName: String): SupportSQLiteOpenHelper {
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(
                InstrumentationRegistry.getInstrumentation().targetContext
            )
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
    }

    private fun createVersion4Schema(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `User_Settings`")
        db.execSQL("DROP TABLE IF EXISTS `User`")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `User` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `password_hash` TEXT NOT NULL,
                `password_salt` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_User_email` ON `User` (`email`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `User_Settings` (
                `user_id` INTEGER NOT NULL,
                `dark_mode_enabled` INTEGER NOT NULL DEFAULT 0,
                `timezone` TEXT NOT NULL DEFAULT 'UTC',
                `notifications_enabled` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`user_id`),
                FOREIGN KEY(`user_id`) REFERENCES `User`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun seedVersion4User(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `User` (`id`, `name`, `email`, `password_hash`, `password_salt`)
            VALUES (1, 'Migration User', 'migration@example.com', 'hash', 'salt')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `User_Settings` (`user_id`, `dark_mode_enabled`, `timezone`, `notifications_enabled`)
            VALUES (1, 1, 'Europe/London', 0)
            """.trimIndent()
        )
    }

    private fun readColumnNames(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            while (cursor.moveToNext()) {
                columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
        }
        return columns
    }
}


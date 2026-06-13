package uws.ac.uk.studymate.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyMateDatabaseMigrationTest {

    // Apply the full migration chain from a seeded v4 schema up to the current
    // version and confirm the User table ends up with every column the current
    // entity expects, plus the v8 indices. (The user rows are intentionally
    // wiped by the 6->7 / 7->8 migrations, so we assert on schema, not data.)
    @Test
    fun migrationFromV4_appliesWholeChain_andEndsAtCurrentUserSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-${System.currentTimeMillis()}.db"

        val helper = createHelper(dbName)
        helper.writableDatabase.use { db ->
            createVersion4Schema(db)
            seedVersion4User(db)
            applyMigrations(db, from = 4, to = 8)

            val userColumns = readColumnNames(db, "User")
            assertTrue(userColumns.contains("id"))
            assertTrue(userColumns.contains("name"))
            assertTrue(userColumns.contains("email"))
            assertTrue(userColumns.contains("password_hash"))
            assertTrue(userColumns.contains("password_salt"))
            assertTrue(userColumns.contains("push_notifications_enabled"))
            assertTrue(userColumns.contains("created_at"))
            assertTrue(userColumns.contains("auth_mode"))

            val indices = readIndexNames(db, "User")
            assertTrue(indices.contains("index_User_name"))
            assertFalse(indices.contains("index_User_email"))

            val settingsColumns = readColumnNames(db, "User_Settings")
            assertEquals(listOf("user_id", "dark_mode_enabled", "timezone"), settingsColumns)
        }

        context.getDatabasePath(dbName).delete()
    }

    // 6 -> 7 wipes all user data (icon set was replaced; stale icon keys removed).
    @Test
    fun migration6To7_wipesUsers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-${System.currentTimeMillis()}.db"

        val helper = createHelper(dbName)
        helper.writableDatabase.use { db ->
            createVersion6UserSchema(db)
            seedVersion6User(db)
            applyMigrations(db, from = 6, to = 7)

            db.query("SELECT COUNT(*) FROM `User`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }

        context.getDatabasePath(dbName).delete()
    }

    // 7 -> 8 switches to the multi-user / one-bio model:
    //   - drops the email unique index
    //   - adds the auth_mode column (default 'password')
    //   - adds a unique index on name
    @Test
    fun migration7To8_dropsEmailIndex_addsAuthModeAndNameIndex() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-${System.currentTimeMillis()}.db"

        // v7 has the same table shape as v6 (6->7 only deleted rows).
        val helper = createHelper(dbName)
        helper.writableDatabase.use { db ->
            createVersion6UserSchema(db)
            applyMigrations(db, from = 7, to = 8)

            val columns = readColumnNames(db, "User")
            assertTrue(columns.contains("auth_mode"))

            val indices = readIndexNames(db, "User")
            assertTrue(indices.contains("index_User_name"))
            assertFalse(indices.contains("index_User_email"))

            // The new auth_mode column defaults to 'password' for inserts that
            // omit it.
            db.execSQL(
                """
                INSERT INTO `User` (`name`, `email`, `password_hash`, `password_salt`)
                VALUES ('Jamie', 'jamie@example.com', 'hash', 'salt')
                """.trimIndent()
            )
            db.query("SELECT auth_mode FROM `User` WHERE name = 'Jamie'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("password", cursor.getString(0))
            }
        }

        context.getDatabasePath(dbName).delete()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    // Apply each migration in the production MIGRATIONS array, in order, from
    // [from] up to [to]. Mirrors what Room does at runtime.
    private fun applyMigrations(db: SupportSQLiteDatabase, from: Int, to: Int) {
        var current = from
        while (current < to) {
            val migration = StudyMateDatabase.MIGRATIONS.first { it.startVersion == current }
            migration.migrate(db)
            current = migration.endVersion
        }
        db.version = to
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

    // The User table as it stood at versions 6 and 7 (identical shape):
    // all the v6 columns plus the email unique index, no auth_mode.
    private fun createVersion6UserSchema(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `User`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `User` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `password_hash` TEXT NOT NULL,
                `password_salt` TEXT NOT NULL,
                `push_notifications_enabled` INTEGER,
                `created_at` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_User_email` ON `User` (`email`)")
    }

    private fun seedVersion6User(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `User` (`id`, `name`, `email`, `password_hash`, `password_salt`, `created_at`)
            VALUES (1, 'Migration User', 'migration@example.com', 'hash', 'salt', CURRENT_TIMESTAMP)
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

    private fun readIndexNames(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val indices = mutableListOf<String>()
        db.query("PRAGMA index_list(`$tableName`)").use { cursor ->
            while (cursor.moveToNext()) {
                indices += cursor.getString(cursor.getColumnIndexOrThrow("name"))
            }
        }
        return indices
    }
}

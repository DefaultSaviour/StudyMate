package uws.ac.uk.studymate.data.entities

import androidx.room.*
/*//////////////////////
Coded by Jamie Coleman
 11/03/26
 updated 16/04/26
 *//////////////////////
// Represents one user in the User table.
//
// Multi-user, one-bio model:
//   - Multiple accounts can exist on the device; the username is unique.
//   - auth_mode = "password" means the user types a password to sign in.
//   - auth_mode = "biometric_only" means the user has no typed password — only
//     fingerprint/face/screen-lock unlocks them. At most one user may hold this
//     mode; password users may *also* enable biometric as a shortcut.
//
// Email is kept as a placeholder column so existing relations + tests keep
// working without a deeper refactor; it isn't exposed to the user any more.
@Entity(
    tableName = "User",
    indices = [Index(value = ["name"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    @ColumnInfo(name = "auth_mode", defaultValue = "password") val authMode: String = "password",
    @ColumnInfo(name = "push_notifications_enabled") val pushNotificationsEnabled: Boolean? = null,
    // Auto-login: when on (the default), a returning password account is signed
    // back in on cold launch without re-typing the password. Inert for
    // biometric_only accounts (there's no password to bypass).
    @ColumnInfo(name = "auto_login_enabled", defaultValue = "1") val autoLoginEnabled: Boolean = true,
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String? = null
)

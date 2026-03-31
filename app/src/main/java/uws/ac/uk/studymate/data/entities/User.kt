package uws.ac.uk.studymate.data.entities

import androidx.room.*

// Represents one user in the User table.
// The email column has a unique index so no two users can share the same email.
@Entity(
    tableName = "User",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,                                           // Auto-generated unique ID.
    val name: String,                                                                           // The user's display name.
    val email: String,                                                                          // The user's email address (must be unique).
    @ColumnInfo(name = "password_hash") val passwordHash: String,                               // The hashed version of the user's password.
    @ColumnInfo(name = "password_salt") val passwordSalt: String,                               // The salt used when hashing the password.
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String? = null // The date and time the account was created.
)

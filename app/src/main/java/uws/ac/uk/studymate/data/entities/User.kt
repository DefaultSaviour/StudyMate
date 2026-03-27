package uws.ac.uk.studymate.data.entities

import androidx.room.*

@Entity(
    tableName = "User",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String, // Stores the hashed password
    @ColumnInfo(name = "password_salt") val passwordSalt: String, // Stores the salt used to hash the password
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String? = null
)

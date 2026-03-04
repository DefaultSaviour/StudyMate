package uws.ac.uk.studymate.data.entities

import androidx.room.*
import java.time.Instant

@Entity(
    tableName = "User",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String? = null
)

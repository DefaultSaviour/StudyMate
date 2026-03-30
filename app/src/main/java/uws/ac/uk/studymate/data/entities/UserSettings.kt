package uws.ac.uk.studymate.data.entities
import androidx.room.*

@Entity(
    tableName = "User_Settings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSettings(
    @PrimaryKey
    @ColumnInfo(name = "user_id", index = true) val userId: Int,
    @ColumnInfo(name = "notifications_enabled", defaultValue = "1") val notificationsEnabled: Boolean = true,
    @ColumnInfo(name = "dark_mode_enabled", defaultValue = "0") val darkModeEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "'UTC'") val timezone: String = "UTC"
)


package uws.ac.uk.studymate.data.entities
import androidx.room.*
/*//////////////////////
Coded by Jamie Coleman
 11/03/26
  updated 16/04/26
 *//////////////////////
// Represents the settings for one user in the User_Settings table.
// Each record is linked to a user by user_id.
// Deleting the user automatically removes their settings.
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
    @ColumnInfo(name = "user_id") val userId: Int,                                        // The user these settings belong to.
    @ColumnInfo(name = "dark_mode_enabled", defaultValue = "0") val darkModeEnabled: Boolean = false, // Whether dark mode is turned on.
    @ColumnInfo(defaultValue = "'UTC'") val timezone: String = "UTC"                    // The user's chosen timezone.
)

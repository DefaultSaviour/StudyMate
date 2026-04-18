package uws.ac.uk.studymate.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.entities.UserStats
/*//////////////////////
Coded by Jamie Coleman
 24/03/26
 *//////////////////////
// Joins a user with their settings and stats in one object.
// Room fills in the settings and stats automatically using the relationships.
data class UserWithSettingsAndStats(
    @Embedded val user: User,                                        // The user record itself.
    @Relation(parentColumn = "id", entityColumn = "user_id")
    val settings: UserSettings?,                                     // The user's settings, or null if none exist.
    @Relation(parentColumn = "id", entityColumn = "user_id")
    val stats: UserStats?                                            // The user's stats, or null if none exist.
)


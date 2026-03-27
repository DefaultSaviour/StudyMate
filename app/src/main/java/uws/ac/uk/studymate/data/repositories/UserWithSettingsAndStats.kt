package uws.ac.uk.studymate.data.repositories

import androidx.room.Embedded
import androidx.room.Relation
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.entities.UserSettings
import uws.ac.uk.studymate.data.entities.UserStats

data class UserWithSettingsAndStats(
    @Embedded val user: User,
    @Relation(parentColumn = "id", entityColumn = "user_id")
    val settings: UserSettings?,
    @Relation(parentColumn = "id", entityColumn = "user_id")
    val stats: UserStats?
)


// comment in exactly how this works!!
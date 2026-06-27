package uws.ac.uk.studymate.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Custom_Events",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class CustomEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Int,
    
    val title: String,
    
    // Stored as ISO string: yyyy-MM-dd
    val date: String,
    
    // Stored as "HH:mm" in 24-hour format, or null for all-day events
    val time: String? = null,
    
    @ColumnInfo(name = "remind_day_before")
    val remindDayBefore: Boolean = false,
    
    val color: String?,
    
    val icon: String = "event"
)

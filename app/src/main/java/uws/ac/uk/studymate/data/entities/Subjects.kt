package uws.ac.uk.studymate.data.entities

import androidx.room.*

@Entity(
    tableName = "Subjects",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id", index = true) val userId: Int,
    val name: String,
    val color: String?
)

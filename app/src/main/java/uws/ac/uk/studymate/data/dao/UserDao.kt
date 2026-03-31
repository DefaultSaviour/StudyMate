package uws.ac.uk.studymate.data.dao

import androidx.room.*
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.repositories.UserWithSettingsAndStats

@Dao
interface UserDao {

/////////////////////// testing only /////////////////
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>
/////////////////////// testing only ////////////////
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getById(id: Int): User?

    @Query("SELECT * FROM User WHERE email = :email")
    suspend fun getByEmail(email: String): User?


    //////// JOIN ///////////////////////////////////////
    @Transaction
    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getUserWithMeta(id: Int): UserWithSettingsAndStats?
    //////// JOIN ///////////////////////////////////////


}

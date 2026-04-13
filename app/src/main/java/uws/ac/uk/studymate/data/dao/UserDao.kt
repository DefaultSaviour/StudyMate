package uws.ac.uk.studymate.data.dao

import androidx.room.*
import uws.ac.uk.studymate.data.entities.User
import uws.ac.uk.studymate.data.relations.UserWithSettingsAndStats
/*//////////////////////
Coded by Jamie Coleman
 10/03/26
 updated 21/03/26
 *//////////////////////
// Provides database operations for the User table.
@Dao
interface UserDao {

    // Get every user in the database (used for testing only).
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>

    // Save a new user and return their generated ID.
    @Insert
    suspend fun insert(user: User): Long

    // Update an existing user's details.
    @Update
    suspend fun update(user: User)

    // Remove a user from the database.
    @Delete
    suspend fun delete(user: User)

    // Find a single user by their ID, or return null if they do not exist.
    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getById(id: Int): User?

    // Find a single user by their email address, or return null if not found.
    @Query("SELECT * FROM User WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    // Get a user together with their settings and stats in one query.
    @Transaction
    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getUserWithMeta(id: Int): UserWithSettingsAndStats?
}

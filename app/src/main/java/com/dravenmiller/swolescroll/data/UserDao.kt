package com.dravenmiller.swolescroll.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dravenmiller.swolescroll.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // Get the profile as a live stream (Flow) for the UI
    @Query("SELECT * FROM user_profile_table WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    // Get profile as a one-shot (for internal logic like QuestManager)
    @Query("SELECT * FROM user_profile_table WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOneShot(): UserProfile?

    // Insert or Update (Replace ensures we only ever have one row)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userProfile: UserProfile)
}

package com.example.swolescroll.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.swolescroll.model.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // GET ALL: Returns a live stream (Flow) of workouts, sorted by newest first
    @Query("SELECT * FROM workout_table ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    // NEW: Get one specific workout
    @Query("SELECT * FROM workout_table WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: String): Workout?

    @Update
    suspend fun updateWorkout(workout: Workout)

    // SAVE: Insert a workout. If ID exists, replace it (Update).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout)

    // DELETE: Remove a workout
    @Delete
    suspend fun deleteWorkout(workout: Workout)
}

package com.dravenmiller.swolescroll.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dravenmiller.swolescroll.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    // Get all exercises alphabetically (Flow for UI observation)
    @Query("SELECT * FROM exercise_table ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    // 👇 NEW: Simple list fetch for the Database Callback check
    @Query("SELECT * FROM exercise_table")
    fun getAllExercisesList(): List<Exercise>

    @Query("SELECT * FROM exercise_table WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getExerciseByName(name: String): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    // 👇 NEW: Bulk insert for Pre-population
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("UPDATE exercise_table SET name = :newName WHERE name = :oldName")
    suspend fun renameExercise(oldName: String, newName: String)
}

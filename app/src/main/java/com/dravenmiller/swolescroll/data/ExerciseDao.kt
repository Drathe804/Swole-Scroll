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

    // Get all exercises alphabetically so the search list looks nice
    @Query("SELECT * FROM exercise_table ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise_table WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getExerciseByName(name: String): Exercise?

    // Save a new exercise.
    // OnConflictStrategy.IGNORE means: "If this ID already exists, just skip it."
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)
}

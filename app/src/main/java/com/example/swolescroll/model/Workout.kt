package com.example.swolescroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// @Entity = "Make a table for this in the database"
@Entity(tableName = "workout_table")
data class Workout(
    // @PrimaryKey = "This is the unique ID for the row"
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val date: Long = System.currentTimeMillis(),
    val name: String = "New Workout",
    val durationMinutes: Int = 0,

    // Our Converter handles this complex list automatically!
    val exercises: List<WorkoutExercise> = emptyList(),

    // Notes
    val notes: String = "",
)
data class WorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val exercise: Exercise,
    val sets: List<Set>,
    // NEW FIELD
    val note: String? = null,
)


package com.example.swolescroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

//Make it a table
@Entity(tableName = "exercise_table")
data class Exercise (
    // Sorting Method
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String, // Exercise name
    val muscleGroup: String, // Muscle group associated with the exercise
    val pushPull: String = "Push", // Push/pull classification (e.g., push or pull)
    val isSingleSide: Boolean = false, // Whether the exercise is one-sided or both sides
    val type: ExerciseType = ExerciseType.STRENGTH, // Enum representing the type of exercise
    )

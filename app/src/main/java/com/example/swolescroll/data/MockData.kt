package com.example.swolescroll.data

import com.example.swolescroll.model.Exercise
import com.example.swolescroll.model.ExerciseType
import com.example.swolescroll.model.Set
import com.example.swolescroll.model.Workout
import com.example.swolescroll.model.WorkoutExercise

object MockData {

    // 1. Define some Exercises
    val benchPress = Exercise(name = "Bench Press", muscleGroup = "Chest", type = ExerciseType.STRENGTH)
    val squat = Exercise(name = "Back Squat", muscleGroup = "Legs", type = ExerciseType.STRENGTH)
    val deadlift = Exercise(name = "Deadlift", muscleGroup = "Back", type = ExerciseType.STRENGTH)
    val overheadPress = Exercise(name = "Overhead Press", muscleGroup = "Shoulders", type = ExerciseType.STRENGTH)

    // 2. Create a Sample Workout List
    val sampleWorkouts = listOf(
        // WORKOUT 1: The Heavy Push Day
        Workout(
            name = "Chest & Shoulders Destruction",
            durationMinutes = 65,
            exercises = listOf(
                WorkoutExercise(
                    exercise = benchPress,
                    sets = listOf(
                        Set(weight = 135.0, reps = 12, isWarmup = true),
                        Set(weight = 225.0, reps = 8, rpe = 7),
                        Set(weight = 315.0, reps = 3, rpe = 9) // The 3 Plates!
                    )
                ),
                WorkoutExercise(
                    exercise = overheadPress,
                    sets = listOf(
                        Set(weight = 135.0, reps = 10, rpe = 8),
                        Set(weight = 155.0, reps = 8, rpe = 9)
                    )
                )
            ),
        ),

        // WORKOUT 2: Leg Day
        Workout(
            name = "Quadzilla Training",
            durationMinutes = 50,
            exercises = listOf(
                WorkoutExercise(
                    exercise = squat,
                    sets = listOf(
                        Set(weight = 225.0, reps = 5, rpe = 6),
                        Set(weight = 315.0, reps = 5, rpe = 8)
                    )
                )
            ),
        )
    )
}

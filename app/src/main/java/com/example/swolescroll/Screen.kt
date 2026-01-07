package com.example.swolescroll

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object LogWorkout : Screen("log_workout")
    object Detail : Screen("detail/{workoutId}"){
        fun createRoute(workoutId: String) = "detail/$workoutId"
    }
    object Stats : Screen("stats")
    object ExerciseHistory : Screen("exercise_history/{exerciseName}"){
        fun createRoute(exerciseName: String) = "exercise_history/$exerciseName"
    }
}

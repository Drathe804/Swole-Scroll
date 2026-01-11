package com.example.swolescroll

import androidx.compose.runtime.collectAsState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.swolescroll.features.detail.WorkoutDetailScreen
import com.example.swolescroll.features.detail.WorkoutDetailViewModel
import com.example.swolescroll.features.detail.WorkoutDetailViewModelFactory
import com.example.swolescroll.features.home.HomeScreen
import com.example.swolescroll.features.home.HomeViewModel
import com.example.swolescroll.features.home.HomeViewModelFactory
import com.example.swolescroll.features.logworkout.LogWorkoutScreen
import com.example.swolescroll.features.logworkout.LogWorkoutViewModel
import com.example.swolescroll.features.logworkout.LogWorkoutViewModelFactory
import com.example.swolescroll.features.stats.ExerciseHistoryScreen
import com.example.swolescroll.features.stats.ExerciseHistoryViewModel
import com.example.swolescroll.features.stats.ExerciseHistoryViewModelFactory
import com.example.swolescroll.features.stats.StatsScreen
import com.example.swolescroll.features.stats.StatsViewModel
import com.example.swolescroll.features.stats.StatsViewModelFactory
import com.example.swolescroll.ui.theme.SwoleScrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwoleScrollApp(application as SwoleScrollApplication) // Pass the Application
        }
    }
}

@Composable
fun SwoleScrollApp(app: SwoleScrollApplication) {
    SwoleScrollTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = Screen.Home.route) {

            // ROUTE 1: HOME (Still using Mock Data for now - we will fix this next!)
            composable(Screen.Home.route) {
                // Get the Viewmodel
                val dao = app.database.workoutDao()
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(app, app.database)
                )

                // Collect the Live Data
                // "collectAsState" turns the Stream into a List we can use
                val workouts by viewModel.workouts.collectAsState(initial = emptyList())

                // Pass the workouts to the screen
                HomeScreen(
                    viewModel = viewModel,
                    onWorkoutClick = { workout ->
                        navController.navigate(Screen.Detail.createRoute(workout.id))
                    },
                    onFabClick = { navController.navigate(Screen.LogWorkout.route) },
                    onStatsClick = { navController.navigate(Screen.Stats.route) }
                )
            }

            // Detail Route
            composable(
                Screen.Detail.route,
                arguments = listOf(androidx.navigation.navArgument("workoutId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                // Get the Workout ID
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable

                val dao = app.database.workoutDao()
                val viewModel: WorkoutDetailViewModel = viewModel(
                    factory = WorkoutDetailViewModelFactory(dao, workoutId)
                )

                WorkoutDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { workoutId ->
                        navController.navigate("log_workout?workoutId=$workoutId")
                    }
                )
            }

            // ROUTE 2: LOG WORKOUT (Updated for Editing!)
            composable(
                // 1. Update route to accept optional ID
                route = "log_workout?workoutId={workoutId}",
                arguments = listOf(navArgument("workoutId") { nullable = true })
            ) { backStackEntry ->

                // 2. Initialize ViewModel (Using your existing Factory)
                val viewModel: LogWorkoutViewModel = viewModel(
                    factory = LogWorkoutViewModelFactory(
                        application = app,
                        db = app.database,
                    )
                )

                // 3. Get the ID (if it exists)
                val workoutIdToEdit = backStackEntry.arguments?.getString("workoutId")

                // 4. If ID exists, tell ViewModel to load that workout!
                LaunchedEffect(workoutIdToEdit) {
                    if (workoutIdToEdit != null) {
                        viewModel.initializeForEdit(workoutIdToEdit)
                    } else {
                        // Optional: Ensure we are clean if opening "New"
                        // viewModel.resetToNew()
                    }
                }

                // 5. Show the Screen
                LogWorkoutScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaveFinished = {
                        // When save is done, go back home!
                        navController.popBackStack()
                    }
                )
            }


            // Stats Screen
            composable(Screen.Stats.route) {
                val dao = app.database.workoutDao()
                val viewModel: StatsViewModel = viewModel(
                    factory = StatsViewModelFactory(dao)
                )

                StatsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onExerciseClick = { exerciseName ->
                        // Navigate to the History of this Exercise
                        navController.navigate(Screen.ExerciseHistory.createRoute(exerciseName))
                    }
                )
            }

            // Exercise History Screen
            composable(
                route = Screen.ExerciseHistory.route,
                arguments = listOf(navArgument("exerciseName") { type = NavType.StringType })
            ) { backStackEntry ->
                // Get the Exercise Name from Exercise clicked
                val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: return@composable

                val dao = app.database.workoutDao()

                val viewModel: ExerciseHistoryViewModel = viewModel(
                    factory = ExerciseHistoryViewModelFactory(dao, exerciseName)
                )

                // Show the Screen
                ExerciseHistoryScreen(
                    exerciseName = exerciseName,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

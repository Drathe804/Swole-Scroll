package com.dravenmiller.swolescroll.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.WorkoutDao
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Helper class: Holds one day's performance for the list
data class HistoryEntry(
    val date: Long,
    val sets: List<Set>,
    val note: String
)

class ExerciseHistoryViewModel(
    private val dao: WorkoutDao,
    private val exerciseName: String
) : ViewModel() {

    // 1. LIST DATA
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history

    // 2. GRAPH DATA (Raw Exercises)
    private val _graphData = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val graphData: StateFlow<List<WorkoutExercise>> = _graphData

    init {
        // Load everything once when the screen opens
        generateHistory(exerciseName)
    }

    // Combined function to load data for BOTH the Graph and the List
    // We renamed this to be generic since it handles Cardio too now
    fun generateHistory(name: String) {
        viewModelScope.launch {
            // A. Get all workouts once
            val allWorkouts = dao.getAllWorkouts().first()

            // B. Flatten to find every instance of this exercise
            val exercises = allWorkouts.flatMap { workout ->
                workout.exercises
                    .filter { it.exercise.name.trim().equals(name.trim(), ignoreCase = true) }
                    // 👇 CRITICAL: Stamp the date onto the exercise so the Graph can use it!
                    .map { it.copy(workoutDate = workout.date) }
            }

            // C. Update Graph Data (Sorted Oldest -> Newest)
            // The graph component will handle the math (Speed vs 1RM)
            _graphData.value = exercises.sortedBy { it.workoutDate }

            // D. Update List Data (Sorted Newest -> Oldest)
            _history.value = exercises.map {
                HistoryEntry(
                    date = it.workoutDate,
                    sets = it.sets,
                    note = it.note ?: ""
                )
            }.sortedByDescending { it.date }
        }
    }

    // Stub to keep your specific call in init valid if you want to keep the old name
    // But ideally, just use generateHistory above.
    fun generateOneRepMaxHistory(name: String) {
        generateHistory(name)
    }
}

class ExerciseHistoryViewModelFactory(
    private val dao: WorkoutDao,
    private val exerciseName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseHistoryViewModel(dao, exerciseName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

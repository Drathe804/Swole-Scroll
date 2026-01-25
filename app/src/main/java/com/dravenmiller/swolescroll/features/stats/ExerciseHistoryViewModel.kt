package com.dravenmiller.swolescroll.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.WorkoutDao
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.util.OneRepMaxCalculator // 👈 IMPORT THIS
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

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history

    // 👇 1. ADDED: Graph Data State
    private val _graphData = MutableStateFlow<List<GraphPoint>>(emptyList())
    val graphData: StateFlow<List<GraphPoint>> = _graphData

    init {
        loadHistory()
        // 👇 2. ADDED: Trigger the graph calculation immediately
        generateOneRepMaxHistory(exerciseName)
    }

    private fun loadHistory() {
        viewModelScope.launch {
            dao.getAllWorkouts().collect { workouts ->
                val entries = mutableListOf<HistoryEntry>()

                workouts.forEach { workout ->
                    val match = workout.exercises.find {
                        it.exercise.name.equals(exerciseName, ignoreCase = true)
                    }

                    if (match != null) {
                        entries.add(HistoryEntry(
                            workout.date,
                            match.sets,
                            match.note ?: ""
                        ))
                    }
                }
                // Sort by date descending (Newest first) for the list
                _history.value = entries.sortedByDescending { it.date }
            }
        }
    }

    // 👇 3. ADDED: The Missing Calculation Function
    fun generateOneRepMaxHistory(name: String) {
        viewModelScope.launch {
            // A. Get a snapshot of all workouts
            val allWorkouts = dao.getAllWorkouts().first()

            // B. Filter & Calculate
            val points = allWorkouts.mapNotNull { workout ->
                val exerciseEntry = workout.exercises.find {
                    it.exercise.name.equals(name, ignoreCase = true)
                }

                if (exerciseEntry != null) {
                    // Find Best Set of the day (Smart 1RM)
                    val bestSet = exerciseEntry.sets.maxByOrNull { set ->
                        OneRepMaxCalculator.getSmart1RM(set.weight, set.reps)
                    }

                    if (bestSet != null && bestSet.weight > 0.0) {
                        val estimates = OneRepMaxCalculator.getAllEstimates(bestSet.weight, bestSet.reps)

                        // Label the formula used
                        val label = when {
                            bestSet.reps <= 5 -> "Brzycki"
                            bestSet.reps <= 12 -> "Epley"
                            else -> "Lombardi"
                        }

                        GraphPoint(
                            date = workout.date,
                            estimates = estimates,
                            formulaLabel = label
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }.sortedBy { it.date } // Sort Oldest -> Newest for the Graph

            _graphData.value = points
        }
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

package com.dravenmiller.swolescroll.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.WorkoutDao
import com.dravenmiller.swolescroll.model.Set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            // 1. Get ALL workouts from the vault
            dao.getAllWorkouts().collect { workouts ->
                val entries = mutableListOf<HistoryEntry>()

                // 2. Hunt for the specific exercise
                workouts.forEach { workout ->
                    // Check: Did we do "Squats" (or whatever exerciseName is) in this workout?
                    val match = workout.exercises.find {
                        it.exercise.name.equals(exerciseName, ignoreCase = true)
                    }

                    // If yes, add it to the history list
                    if (match != null) {
                        entries.add(HistoryEntry(
                            workout.date,
                            match.sets,
                            match.note ?: ""
                        ))
                    }
                }

                // 3. Update the UI
                _history.value = entries
            }
        }
    }
}

// The Factory: Needed because we are passing a custom argument (exerciseName) to the ViewModel
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

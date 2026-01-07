package com.example.swolescroll.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swolescroll.data.WorkoutDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 1. Update the Data Class to hold Reps
data class PersonalRecord(
    val exerciseName: String,
    val maxWeight: Double,
    val maxReps: Int // <--- NEW FIELD
)

class StatsViewModel(private val dao: WorkoutDao) : ViewModel() {

    private val _prList = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val prList: StateFlow<List<PersonalRecord>> = _prList

    init {
        calculateStats()
    }

    private fun calculateStats() {
        viewModelScope.launch {
            dao.getAllWorkouts().collect { workouts ->
                // We map the Name to a Pair of (Weight, Reps)
                val maxes = mutableMapOf<String, Pair<Double, Int>>()

                workouts.forEach { workout ->
                    workout.exercises.forEach { workoutExercise ->
                        val exerciseName = workoutExercise.exercise.name

                        // 2. Find the heaviest SET (not just the weight number)
                        val bestSet = workoutExercise.sets.maxByOrNull { it.weight }

                        if (bestSet != null) {
                            val currentMax = maxes[exerciseName]

                            // 3. If this set is heavier than what we have on file, replace it!
                            // (Or if it's the first time we've seen this exercise)
                            if (currentMax == null || bestSet.weight > currentMax.first) {
                                maxes[exerciseName] = Pair(bestSet.weight, bestSet.reps)
                            }
                        }
                    }
                }

                // 4. Convert to List
                val records = maxes.map { (name, data) ->
                    PersonalRecord(name, data.first, data.second)
                }.sortedBy { it.exerciseName }

                _prList.value = records
            }
        }
    }
}

class StatsViewModelFactory(private val dao: WorkoutDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

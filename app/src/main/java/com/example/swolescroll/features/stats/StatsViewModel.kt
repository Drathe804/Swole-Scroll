package com.example.swolescroll.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swolescroll.data.WorkoutDao
import com.example.swolescroll.model.ExerciseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 1. DATA CLASS (Holds the pre-formatted text)
data class PersonalRecord(
    val exerciseName: String,
    val type: ExerciseType,
    val mainText: String,   // e.g. "315 lbs"
    val subText: String     // e.g. "x 5" or "for 30 yds"
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
                val records = mutableListOf<PersonalRecord>()

                // Group by Exercise Name
                val groupedExercises = workouts
                    .flatMap { it.exercises }
                    .groupBy { it.exercise.name }

                groupedExercises.forEach { (name, history) ->
                    // Determine Type (Safe fallback)
                    val type = history.firstOrNull()?.exercise?.type ?: ExerciseType.STRENGTH

                    // Flatten all sets to find the best single performance
                    val allSets = history.flatMap { it.sets }

                    if (allSets.isNotEmpty()) {
                        when (type) {
                            // 🏃‍♂️ CARDIO (Max Distance Session)
                            ExerciseType.CARDIO -> {
                                // Find session with max TOTAL distance
                                val bestSession = history.maxByOrNull { session ->
                                    session.sets.sumOf { it.distance ?: 0.0 }
                                }

                                if (bestSession != null) {
                                    val totalDist = bestSession.sets.sumOf { it.distance ?: 0.0 }
                                    val totalSeconds = bestSession.sets.sumOf { it.time ?: 0 }

                                    // Avg Speed
                                    val hours = totalSeconds / 3600.0
                                    val avgSpeed = if (hours > 0) totalDist / hours else 0.0

                                    // Dominant Level
                                    val dominantSetGroup = bestSession.sets
                                        .groupBy { it.weight }
                                        .maxByOrNull { entry -> entry.value.sumOf { it.time ?: 0 } }
                                    val domLevel = dominantSetGroup?.key ?: 0.0

                                    val mainStr = String.format("%.2f mi", totalDist)
                                    val speedStr = String.format("%.2f mph", avgSpeed)
                                    val subStr = "$speedStr (@ Lvl $domLevel)"

                                    records.add(PersonalRecord(name, type, mainStr, subStr))
                                }
                            }

                            // 🚛 LOADED CARRY (Max Weight -> Tie breaker: Max Distance)
                            ExerciseType.LoadedCarry -> {
                                val bestSet = allSets.maxWithOrNull(
                                    compareBy<com.example.swolescroll.model.Set> { it.weight }
                                        .thenBy { it.distance ?: 0.0 }
                                )

                                if (bestSet != null) {
                                    val mainStr = "${bestSet.weight} lbs"
                                    val subStr = "for ${bestSet.distance ?: 0.0} yds"
                                    records.add(PersonalRecord(name, type, mainStr, subStr))
                                }
                            }

                            // 🧘 ISOMETRIC (Max Weight -> Tie breaker: Max Time)
                            ExerciseType.ISOMETRIC -> {
                                val bestSet = allSets.maxWithOrNull(
                                    compareBy<com.example.swolescroll.model.Set> { it.weight }
                                        .thenBy { it.time ?: 0 }
                                )

                                if (bestSet != null) {
                                    val mainStr = "${bestSet.weight} lbs"
                                    // Use the helper formatted time (e.g. "1:30")
                                    val subStr = "for ${bestSet.timeFormatted()}"
                                    records.add(PersonalRecord(name, type, mainStr, subStr))
                                }
                            }

                            // 🏋️‍♂️ STRENGTH (Standard: Max Weight -> Max Reps)
                            else -> {
                                val bestSet = allSets.maxWithOrNull(
                                    compareBy<com.example.swolescroll.model.Set> { it.weight }
                                        .thenBy { it.reps }
                                )

                                if (bestSet != null) {
                                    val mainStr = "${bestSet.weight} lbs"
                                    val subStr = "x ${bestSet.reps}"
                                    records.add(PersonalRecord(name, type, mainStr, subStr))
                                }
                            }
                        }
                    }
                }

                _prList.value = records.sortedBy { it.exerciseName }
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

package com.example.swolescroll.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swolescroll.data.WorkoutDao
import com.example.swolescroll.model.ExerciseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PersonalRecord(
    val exerciseName: String,
    val type: ExerciseType,
    val mainText: String,
    val subText: String
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

                val groupedExercises = workouts
                    .flatMap { it.exercises }
                    .groupBy { it.exercise.name }

                groupedExercises.forEach { (name, history) ->
                    val rawType = history.firstOrNull()?.exercise?.type ?: ExerciseType.STRENGTH

                    // 1. Force "Carry" type if name contains keywords (Safety check)
                    val isCarryName = name.contains("Carry", true) || name.contains("Farmer", true)
                    val isStairsName = name.contains("Stair", true) || name.contains("Step", true)

                    val effectiveType = if (isCarryName) ExerciseType.LoadedCarry else rawType

                    val allSets = history.flatMap { it.sets }

                    if (allSets.isNotEmpty()) {
                        when {
                            // 🏃‍♂️ CARDIO (Treadmill, Bike, Stairs)
                            effectiveType == ExerciseType.CARDIO -> {
                                val bestSession = history.maxByOrNull { session ->
                                    session.sets.sumOf { it.distance ?: 0.0 }
                                }

                                if (bestSession != null) {
                                    val totalDist = bestSession.sets.sumOf { it.distance ?: 0.0 }
                                    val totalSeconds = bestSession.sets.sumOf { it.time ?: 0 }

                                    val domGroup = bestSession.sets
                                        .groupBy { it.weight }
                                        .maxByOrNull { entry -> entry.value.sumOf { it.time ?: 0 } }
                                    val domLevel = domGroup?.key ?: 0.0

                                    if (isStairsName) {
                                        // 🪜 STAIRS LOGIC (Steps & Steps/Min)
                                        // Fixes "4000 mph" error 🛠️
                                        val minutes = totalSeconds / 60.0
                                        val spm = if (minutes > 0) totalDist / minutes else 0.0

                                        // Display as Integer (no decimals for steps)
                                        val mainStr = "${totalDist.toInt()} steps"
                                        val subStr = "${spm.toInt()} steps/min (@ Lvl ${domLevel.toInt()})"
                                        records.add(PersonalRecord(name, effectiveType, mainStr, subStr))
                                    } else {
                                        // 🏃‍♂️ RUNNING LOGIC (Miles & MPH)
                                        val hours = totalSeconds / 3600.0
                                        val avgSpeed = if (hours > 0) totalDist / hours else 0.0

                                        val mainStr = String.format("%.2f mi", totalDist)
                                        val speedStr = String.format("%.2f mph", avgSpeed)
                                        val subStr = "$speedStr (@ Lvl $domLevel)"
                                        records.add(PersonalRecord(name, effectiveType, mainStr, subStr))
                                    }
                                }
                            }

                            // 🚛 LOADED CARRY (Farmer Carries)
                            effectiveType == ExerciseType.LoadedCarry -> {
                                val bestSet = allSets.maxWithOrNull(
                                    compareBy<com.example.swolescroll.model.Set> { it.weight }
                                        .thenBy { it.distance ?: 0.0 }
                                )

                                if (bestSet != null) {
                                    val mainStr = "${bestSet.weight} lbs"
                                    val subStr = "for ${bestSet.distance ?: 0.0} yds"
                                    records.add(PersonalRecord(name, effectiveType, mainStr, subStr))
                                }
                            }

                            // 🧘 ISOMETRIC
                            effectiveType == ExerciseType.ISOMETRIC -> {
                                val bestSet = allSets.maxWithOrNull(
                                    compareBy<com.example.swolescroll.model.Set> { it.weight }
                                        .thenBy { it.time ?: 0 }
                                )
                                if (bestSet != null) {
                                    val mainStr = "${bestSet.weight} lbs"
                                    val subStr = "for ${bestSet.timeFormatted()}"
                                    records.add(PersonalRecord(name, effectiveType, mainStr, subStr))
                                }
                            }

                            // 🏋️‍♂️ STRENGTH
                            else -> {
                                val bestSet = allSets.maxWithOrNull(
                                    compareBy<com.example.swolescroll.model.Set> { it.weight }
                                        .thenBy { it.reps }
                                )
                                if (bestSet != null) {
                                    val mainStr = "${bestSet.weight} lbs"
                                    val subStr = "x ${bestSet.reps}"
                                    records.add(PersonalRecord(name, effectiveType, mainStr, subStr))
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

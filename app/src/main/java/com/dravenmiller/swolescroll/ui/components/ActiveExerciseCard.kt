package com.dravenmiller.swolescroll.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.features.logworkout.LogWorkoutViewModel
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise

@Composable
fun ActiveExerciseCard(
    workoutExercise: WorkoutExercise,
    index: Int,
    expandedIndex: Int,
    isFocusMode: Boolean,
    isSiegeModeEnabled: Boolean,
    userWeight: Double,
    prMap: Map<String, String>,
    historyMap: Map<String, List<String>>,
    monsterHpMap: Map<String, Int>,
    monsterImageId: Int,
    viewModel: LogWorkoutViewModel,
    onExpandedIndexChange: (Int) -> Unit,
    onShowDistanceDialog: (String) -> Unit,
    onDeleteClick: () -> Unit // 👈 ADDED THIS PARAMETER!
) {
    if (isFocusMode && expandedIndex != index) return

    val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH
    val isDungeon = workoutExercise.supersetId != null
    val thisHistory = historyMap[workoutExercise.exercise.name] ?: emptyList()
    val historyPrString = prMap[workoutExercise.exercise.name]

    val currentBestValue = remember(workoutExercise.sets, workoutExercise.exercise.type) {
        when (type) {
            ExerciseType.CARDIO -> workoutExercise.sets.sumOf { it.distance ?: 0.0 }
            ExerciseType.LoadedCarry -> workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
            else -> workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
        }
    }

    val bestSetToday = workoutExercise.sets.maxByOrNull { it.weight }
    val currentBestWeight = bestSetToday?.weight ?: 0.0
    val currentBestReps = bestSetToday?.reps ?: 0

    val isNewRecord = remember(workoutExercise.sets, historyPrString, type) {
        when (type) {
            ExerciseType.CARDIO -> {
                val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }
                if (totalDist > 0 && totalSeconds > 0) {
                    val isStairs = workoutExercise.exercise.name.contains("stairs", ignoreCase = true)
                    val currentSpeed = if (isStairs) {
                        val minutes = totalSeconds / 60.0
                        if (minutes > 0) totalDist / minutes else 0.0
                    } else {
                        val hours = totalSeconds / 3600.0
                        if (hours > 0) totalDist / hours else 0.0
                    }
                    val rawHistory = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                    val historySpeed = if (isStairs && historyPrString?.contains("mph") == true) {
                        rawHistory / 60.0
                    } else {
                        rawHistory
                    }
                    currentSpeed > historySpeed
                } else false
            }
            ExerciseType.LoadedCarry -> {
                val currentW = bestSetToday?.weight ?: 0.0
                val currentD = bestSetToday?.distance ?: 0.0
                val historyW = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                val historyD = historyPrString?.substringAfter("for ")?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                if (currentD > 0) {
                    val isHeavier = currentW > historyW
                    val isFurther = (currentW == historyW) && (currentW > 0) && (currentD > historyD)
                    isHeavier || isFurther
                } else false
            }
            ExerciseType.ISOMETRIC -> {
                val maxWeight = workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
                val maxTime = workoutExercise.sets.maxOfOrNull { it.time } ?: 0
                val historyWeight = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                maxWeight > historyWeight && maxWeight > 0 && maxTime > 0
            }
            else -> {
                val w = bestSetToday?.weight ?: 0.0
                val r = bestSetToday?.reps ?: 0
                val histWeight = historyPrString?.split("x")?.firstOrNull()?.replace("lbs", "")?.trim()?.toDoubleOrNull() ?: 0.0
                val histReps = historyPrString?.split("x")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                if (r > 0) {
                    val isWeightPR = w > histWeight
                    val isRepPR = (w == histWeight) && (w > 0) && (r > histReps)
                    isWeightPR || isRepPR
                } else false
            }
        }
    }

    var displayPr = historyPrString
    if (type == ExerciseType.CARDIO && workoutExercise.exercise.name.contains("stairs", ignoreCase = true) && historyPrString?.contains("mph") == true) {
        val raw = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
        displayPr = "${String.format("%.2f", raw / 60.0)} stairs/min"
    }

    if (isNewRecord) {
        displayPr = when (type) {
            ExerciseType.CARDIO -> {
                val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }
                val isStairs = workoutExercise.exercise.name.contains("stairs", ignoreCase = true)
                if (isStairs) {
                    val minutes = totalSeconds / 60.0
                    "${String.format("%.2f", if (minutes > 0) totalDist / minutes else 0.0)} stairs/min"
                } else {
                    val hours = totalSeconds / 3600.0
                    "${String.format("%.2f", if (hours > 0) totalDist / hours else 0.0)} mph"
                }
            }
            ExerciseType.LoadedCarry -> "${bestSetToday?.weight} lbs for ${bestSetToday?.distance} yds"
            else -> "$currentBestWeight lbs x $currentBestReps"
        }
    }

    Column(modifier = Modifier.animateContentSize()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDungeon) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Column {
                if (isDungeon) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Linked", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text("Mini-Dungeon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                EditExerciseItem(
                    workoutExercise = workoutExercise,
                    exerciseIndex = index,
                    userWeight = userWeight,
                    monsterHp = monsterHpMap[workoutExercise.exercise.name] ?: 2000,
                    monsterImageId = monsterImageId,
                    isSiegeModeEnabled = isSiegeModeEnabled && (expandedIndex != index),
                    isExpanded = expandedIndex == index,
                    personalRecord = displayPr,
                    isNewPr = isNewRecord,
                    pastNotes = thisHistory,
                    onDelete = {
                        // 👇 Check if there is ANY logged weight, reps, distance, or time
                        val hasLoggedData = workoutExercise.sets.any { set ->
                            set.weight > 0.0 || set.reps > 0 || (set.distance ?: 0.0) > 0.0 || (set.time ?: 0) > 0
                        }

                        if (!hasLoggedData) {
                            // If it's all zeros, delete it instantly!
                            viewModel.addedExercises.remove(workoutExercise)
                            onExpandedIndexChange(-1)
                        } else {
                            // Only show the warning if they actually did work
                            onDeleteClick()
                        }
                    },
                    onInfoClick = {
                        viewModel.loadHistory(workoutExercise.exercise.id, workoutExercise.exercise.name)
                        viewModel.showHistoryDialog.value = true
                    },
                    onHeaderClick = {
                        onExpandedIndexChange(if (expandedIndex == index) -1 else index)
                    },
                    onAddSet = {
                        val newSet = Set(weight = 0.0, reps = 0)
                        viewModel.addedExercises[index] = workoutExercise.copy(sets = workoutExercise.sets + newSet)
                    },
                    onUpdateSet = { setIndex, updatedSet ->
                        val updatedSets = workoutExercise.sets.toMutableList()
                        updatedSets[setIndex] = updatedSet
                        viewModel.addedExercises[index] = workoutExercise.copy(sets = updatedSets)
                    },
                    onRemoveSet = { setIndex ->
                        val updatedSets = workoutExercise.sets.toMutableList()
                        updatedSets.removeAt(setIndex)
                        viewModel.addedExercises[index] = workoutExercise.copy(sets = updatedSets)
                    },
                    onUpdateNote = { newNote ->
                        viewModel.addedExercises[index] = workoutExercise.copy(note = newNote)
                    },
                    onTreadmillSplit = { seconds, incline, level ->
                        viewModel.splitCardioSet(workoutExercise.id, workoutExercise.sets.lastIndex, seconds, incline, level)
                        if (incline == 0.0 && workoutExercise.sets.isNotEmpty()) {
                            onShowDistanceDialog(workoutExercise.id)
                        }
                    },
                    backgroundColor = if (isDungeon) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

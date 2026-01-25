package com.dravenmiller.swolescroll.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise

@Composable
fun DetailExerciseItem(
    workoutExercise: WorkoutExercise
) {
    // STATE: Is the dropdown open?
    var expanded by remember { mutableStateOf(false) }

    // Safe Type Fallback
    val safeType = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

    // 1. FILTER: Create a clean list for display 🧹
    // If Cardio, only show sets with actual movement.
    val visibleSets = remember(workoutExercise.sets) {
        if (safeType == ExerciseType.CARDIO) {
            workoutExercise.sets.filter { (it.distance ?: 0.0) > 0.0 }
        } else {
            workoutExercise.sets
        }
    }

    // 2. SMART SUMMARY HEADER 🧠
    val summaryText = remember(workoutExercise.sets) {
        if (safeType == ExerciseType.CARDIO) {
            // A. Totals
            val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
            val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }

            // Format Distance
            val distStr = String.format("%.2f", totalDist).trimEnd('0').trimEnd('.')

            // B. Majority Logic
            // Group by settings -> Find which one had the most TIME.
            val dominantSetGroup = workoutExercise.sets
                .groupBy { Pair(it.reps, it.weight) } // Pair(Reps, Weight)
                .entries
                .maxWithOrNull(
                    compareBy<Map.Entry<Pair<Int, Double>, List<Set>>> { entry ->
                        entry.value.sumOf { it.time ?: 0 }
                    }.thenBy { entry ->
                        entry.value.sumOf { it.distance ?: 0.0 }
                    }
                )

            // 💡 VARIABLE MAPPING:
            // first = Reps (Used for Incline now)
            // second = Weight (Used for LEVEL now)
            val domReps = dominantSetGroup?.key?.first ?: 0
            val domWeight = dominantSetGroup?.key?.second ?: 0.0

            // C. Format Time
            val m = totalSeconds / 60
            val s = totalSeconds % 60
            val timeStr = String.format("%d:%02d", m, s)

            // D. Format Intensity (THE FIX 🛠️)
            val intensityParts = mutableListOf<String>()
            val isTreadmill = workoutExercise.exercise.name.contains("Treadmill", true)

            // 1. Level (Always from Weight)
            if (domWeight > 0) intensityParts.add("Lvl $domWeight")

            // 2. Incline (Only for Treadmill, from Reps)
            if (isTreadmill && domReps > 0) {
                intensityParts.add("${domReps/10.0}% Inc")
            }

            val intStr = if(intensityParts.isNotEmpty()) " (@ ${intensityParts.joinToString(", ")})" else ""

            val isStairs = workoutExercise.exercise.name.contains("Stair", true)
            val unit = if (isStairs) "stairs" else "mi"

            "Total: $distStr $unit in $timeStr$intStr"
        }
        else {
            // Standard "Top Set" for Strength
            val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
            if (bestSet != null) {
                formatDetailSet(bestSet, safeType, workoutExercise.exercise.name)
            } else null
        }
    }

    // 3. VOLUME CALCULATION (Same as before)
    val totalVolume = remember(workoutExercise.sets, workoutExercise.exercise.isSingleSide) {
        workoutExercise.sets.sumOf { set ->
            val multiplier = if (workoutExercise.exercise.isSingleSide) 2 else 1
            val w = set.weight
            val d = set.distance ?: 0.0
            val t = set.time ?: 0

            when(safeType) {
                ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                ExerciseType.TWENTY_ONES -> {
                    val rawVol = (w * set.reps * multiplier)
                    ((rawVol * 2)/3).toInt()
                }
                else -> 0
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- HEADER ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workoutExercise.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // The Summary Line
                    if (summaryText != null) {
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (totalVolume > 0) {
                        Text(
                            text = "Volume: ${java.text.NumberFormat.getIntegerInstance().format(totalVolume)} lbs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${visibleSets.size} Sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- EXPANDABLE DROPDOWN ---
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Iterate over ONLY the visible sets
                    visibleSets.forEachIndexed { index, set ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Set ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDetailSet(set, safeType, workoutExercise.exercise.name),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (!workoutExercise.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notes: ${workoutExercise.note ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// 🛠️ HELPER: Formats the set string based on Type
// UPDATED: Now respects that Weight = Lvl and Reps = Incline
fun formatDetailSet(set: Set, type: ExerciseType, name: String): String {
    val isTreadmill = name.contains("Treadmill", true) || name.contains("Run", true)
    val isStairs = name.contains("Stair", true) || name.contains("Step", true)

    return when(type) {
        ExerciseType.CARDIO -> {
            val dist = set.distance ?: 0.0
            val distUnit = if(isStairs) "stairs" else "mi"
            val time = set.timeFormatted()

            // 1. Main Level (Always Weight)
            val level = set.weight
            val details = mutableListOf<String>()

            if (level > 0) details.add("Lvl $level")

            // 2. Treadmill Incline (Always Reps)
            if (isTreadmill) {
                val inc = set.reps / 10.0
                if (inc > 0) details.add("$inc% Inc")
            }

            val detailStr = if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
            "$dist $distUnit in $time$detailStr"
        }
        ExerciseType.ISOMETRIC -> "${set.weight} lbs for ${set.timeFormatted()}"
        ExerciseType.LoadedCarry -> "${set.weight} lbs for ${set.distance ?: 0.0} yds"
        else -> "${set.weight} lbs x ${set.reps} reps"
    }
}

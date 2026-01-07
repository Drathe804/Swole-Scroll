package com.example.swolescroll.ui.components

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
import com.example.swolescroll.model.ExerciseType
import com.example.swolescroll.model.Set
import com.example.swolescroll.model.WorkoutExercise

@Composable
fun DetailExerciseItem(
    workoutExercise: WorkoutExercise
) {
    // STATE: Is the dropdown open?
    var expanded by remember { mutableStateOf(false) }

    // 1. SMART VOLUME CALCULATION 🧮
    // Different math for different types
    val totalVolume = remember(workoutExercise.sets, workoutExercise.exercise.isSingleSide) {
        workoutExercise.sets.sumOf { set ->
            val multiplier = if (workoutExercise.exercise.isSingleSide) 2 else 1
            val w = set.weight
            val d = set.distance ?: 0.0
            val t = set.time ?: 0

            when(workoutExercise.exercise.type) {
                ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                else -> 0 // Cardio doesn't usually track "Volume" in lbs
            }
        }
    }

    // 2. SMART "BEST SET" FINDER 🏆
    // Cardio/Carry = Max Distance, Strength/Iso = Max Weight
    val bestSet = remember(workoutExercise.sets) {
        when(workoutExercise.exercise.type) {
            ExerciseType.CARDIO, ExerciseType.LoadedCarry -> workoutExercise.sets.maxByOrNull { it.distance ?: 0.0 }
            else -> workoutExercise.sets.maxByOrNull { it.weight }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }, // Toggle expand on click
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- HEADER ROW (Always Visible) ---
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
                    if (bestSet != null) {
                        // 👇 USE HELPER FUNCTION HERE
                        val summaryText = formatDetailSet(bestSet, workoutExercise.exercise.type, workoutExercise.exercise.name)
                        Text(
                            text = "Top Set: $summaryText",
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

                // The Arrow Icon and Set Count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${workoutExercise.sets.size} Sets",
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

            // --- EXPANDABLE DROPDOWN (Visible only when clicked) ---
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // List every set
                    workoutExercise.sets.forEachIndexed { index, set ->
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
                            // 👇 USE HELPER FUNCTION HERE
                            Text(
                                text = formatDetailSet(set, workoutExercise.exercise.type, workoutExercise.exercise.name),
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
fun formatDetailSet(set: Set, type: ExerciseType, name: String): String {
    val isTreadmill = name.contains("Treadmill", true) || name.contains("Run", true)
    val isStairs = name.contains("Stair", true) || name.contains("Step", true)

    return when(type) {
        ExerciseType.CARDIO -> {
            val dist = set.distance ?: 0.0
            val distUnit = if(isStairs) "flr" else "mi"
            val time = set.timeFormatted()

            if (isTreadmill) {
                // Treadmill: Dist | Time | Inc%
                // (Note: Level is stored in reps * 10, but often irrelevant for history if speed varies)
                "$dist $distUnit in $time (${set.weight}% inc)"
            } else {
                // Bike: Dist | Time | Lvl
                "$dist $distUnit in $time (Lvl ${set.weight})"
            }
        }
        ExerciseType.ISOMETRIC -> "${set.weight} lbs for ${set.timeFormatted()}"
        ExerciseType.LoadedCarry -> "${set.weight} lbs for ${set.distance ?: 0.0} yds"
        else -> "${set.weight} lbs x ${set.reps} reps"
    }
}

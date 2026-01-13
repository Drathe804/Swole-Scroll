package com.example.swolescroll.features.logworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.swolescroll.model.ExerciseType
import com.example.swolescroll.model.WorkoutExercise

@Composable
fun ExerciseHistoryDialog(
    exerciseName: String,
    history: List<WorkoutExercise>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            if (history.isEmpty()) {
                Text("No history found.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(history) { entry ->
                        HistoryItem(entry)
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun HistoryItem(entry: WorkoutExercise) {
    Column {
        // 🛡️ CRASH FIX: Force a default type if it is null in the database
        val safeType = entry.exercise.type ?: ExerciseType.STRENGTH

        // 1. Header (Set Count)
        Text(
            text = "Sets: ${entry.sets.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // 2. SMART DISPLAY LOGIC 🧠 (Using safeType now)
        if (safeType == ExerciseType.CARDIO) {
            // --- CARDIO: SHOW TOTALS ONLY ---
            val totalDist = entry.sets.sumOf { it.distance ?: 0.0 }
            val totalTime = entry.sets.sumOf { it.time ?: 0 }

            val min = totalTime / 60
            val sec = totalTime % 60
            val timeStr = String.format("%d:%02d", min, sec)

            // Format distance to remove trailing zeros (e.g. "1.0" -> "1")
            val distStr = String.format("%.2f", totalDist).removeSuffix("0").removeSuffix(".")

            val isStairs = entry.exercise.name.contains("Stair", true)
            val unit = if (isStairs) "stairs" else "mi"

            Text(
                text = "Total: $distStr $unit in $timeStr",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

        } else {
            // --- OTHERS: LIST EVERY SET ---
            entry.sets.forEach { set ->
                val text = when (safeType) { // Using safeType here too!
                    ExerciseType.LoadedCarry -> {
                        "${set.weight} lbs for ${set.distance ?: 0.0} yds"
                    }
                    ExerciseType.ISOMETRIC -> {
                        val time = set.time ?: 0
                        "${set.weight} lbs for ${time}s"
                    }
                    ExerciseType.TWENTY_ONES -> {
                        "${set.weight} lbs (21s)"
                    }
                    // STRENGTH (Default)
                    else -> {
                        "${set.weight} lbs x ${set.reps}"
                    }
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

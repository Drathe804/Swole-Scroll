package com.example.swolescroll.features.logworkout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
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
import com.example.swolescroll.model.WorkoutExercise

@Composable
fun ExerciseHistoryDialog(
    exerciseName: String,
    history: List<WorkoutExercise>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = exerciseName, style = MaterialTheme.typography.headlineSmall)
                Text(text = "History", style = MaterialTheme.typography.labelSmall)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ){
                if (history.isEmpty()){
                    item {
                        Text(text = "No history yet. Time to make some!")
                    }
                } else {
                    items(items = history){ entry ->
                        HistoryRow(entry)
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun HistoryRow(entry: WorkoutExercise) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Sets: ${entry.sets.size}", fontWeight = FontWeight.Bold)
        entry.sets.forEach { set ->
            Text("  ${set.weight} lbs x ${set.reps}")
        }
    }
}
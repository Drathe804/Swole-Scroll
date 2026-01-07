package com.example.swolescroll.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.swolescroll.data.MockData
import com.example.swolescroll.model.WorkoutExercise

@Composable
fun ExerciseItem(
    workoutExercise: WorkoutExercise,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Exercise Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workoutExercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = workoutExercise.exercise.muscleGroup,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Summary (e.g., "3 Sets")
            Text(
                text = "${workoutExercise.sets.size} Sets",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        // A thin line separator below each item
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseItemPreview() {
    // Look how easy MockData makes this!
    val sampleExercise = MockData.sampleWorkouts[0].exercises[0]
    ExerciseItem(workoutExercise = sampleExercise)
}

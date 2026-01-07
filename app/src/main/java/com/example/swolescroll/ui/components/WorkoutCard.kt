package com.example.swolescroll.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.swolescroll.data.MockData
import com.example.swolescroll.model.Workout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutCard(
    workout: Workout,
    modifier: Modifier = Modifier
) {
    // Design the Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ROW 1: Workout Name
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ROW 2: Date and Exercise Count
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Format the date (e.g., "Nov 19")
                val dateString = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(workout.date))

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(text = " • ") // A little separator dot

                Text(
                    text = "${workout.exercises.size} Exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// THIS is why we made MockData!
// You can see exactly what it looks like without running the app.
@Preview
@Composable
fun WorkoutCardPreview() {
    WorkoutCard(workout = MockData.sampleWorkouts[0])
}

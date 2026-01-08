package com.example.swolescroll.features.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.swolescroll.model.ExerciseType
import com.example.swolescroll.ui.components.DetailExerciseItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onBackClick: () -> Unit
) {
    val workout = viewModel.workout.value
    var isEditingTitle by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val totalWorkoutVolume = remember(workout?.exercises) {
        workout?.exercises?.sumOf { workoutExercise ->
            workoutExercise.sets.sumOf { set ->
                val multiplier = if (workoutExercise.exercise.isSingleSide) 2 else 1
                val w = set.weight
                val d = set.distance ?: 0.0
                val t = set.time ?: 0
                val safeType = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                when (safeType) {
                    ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                    ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                    ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                    else -> 0
                }
            }
        } ?: 0
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWorkout(onDeleted = onBackClick)
                    showDeleteDialog = false
                }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isEditingTitle) {
                        var tempName by remember { mutableStateOf(workout?.name ?: "") }
                        androidx.compose.material3.OutlinedTextField(
                            value = tempName,
                            onValueChange = { newName ->
                                tempName = newName
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    viewModel.updateWorkoutName(tempName)
                                    isEditingTitle = false
                                }
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.updateWorkoutName(tempName)
                                    isEditingTitle = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = workout?.name ?: "Loading...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                if (workout != null) {
                                    isEditingTitle = true
                                }
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (workout == null) {
            // Loading State
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Loading scroll...")
            }
        } else {
            // Content State
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Header Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalWorkoutVolume > 0) {
                        Text(
                            text = "Total Volume: ${
                                java.text.NumberFormat.getIntegerInstance()
                                    .format(totalWorkoutVolume)
                            } lbs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val dateString = SimpleDateFormat(
                        "EEEE, MMM dd",
                        Locale.getDefault()
                    ).format(Date(workout.date))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Workout Notes Display
                if (workout.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Workout Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = workout.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }

                // The List of Exercises
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(workout.exercises) { workoutExercise ->
                        DetailExerciseItem(workoutExercise = workoutExercise)
                    }

                }
            }
        }
    }
}

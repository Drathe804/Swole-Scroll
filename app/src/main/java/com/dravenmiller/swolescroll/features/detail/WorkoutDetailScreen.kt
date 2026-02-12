package com.dravenmiller.swolescroll.features.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.calculateTotalVolume
import com.dravenmiller.swolescroll.ui.components.DetailExerciseItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val workout = viewModel.workout.value
    var isEditingTitle by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val userWeight by viewModel.userWeight.collectAsState(initial = 0.0)


    val totalWorkoutVolume = remember(workout?.exercises, userWeight) {
        workout?.exercises?.sumOf { workoutExercise ->
            // Use the helper function we created in WorkoutExercise.kt
            workoutExercise.calculateTotalVolume(userWeight)
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
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
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
                            onValueChange = { newName -> tempName = newName },
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
                // 2. NEW EDIT/DELETE BUTTONS
                actions = {
                    if (workout != null) {
                        IconButton(onClick = { onEditClick(workout.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Workout")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Workout", tint = MaterialTheme.colorScheme.error)
                        }
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
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Loading scroll...")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 3. RESTORED HEADER INFO
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalWorkoutVolume > 0) {
                        Text(
                            text = "Total Volume: ${java.text.NumberFormat.getIntegerInstance().format(totalWorkoutVolume)} lbs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val dateString = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(workout.date))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!workout.notes.isNullOrBlank()) {
                    CollapsibleNoteCard(note = workout.notes)
                }

                // 5. RESTORED LIST
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(workout.exercises) { workoutExercise ->
                        val vol = workoutExercise.calculateTotalVolume(userWeight)
                        DetailExerciseItem(
                            workoutExercise = workoutExercise,
                            userWeight = userWeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleNoteCard(
    note: String,
    modifier: Modifier = Modifier
) {
    // 1. State to track if we are expanded
    var isExpanded by remember { mutableStateOf(false) }

    // 2. Simple logic: If text is > 150 chars, we treat it as "Long"
    val isLongText = remember(note) { note.length > 150 }

    Card(
        // Use your theme's container color (the beige/tan color)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            // OR use your specific hex if you have it, e.g., Color(0xFFEFE5D5)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            // 3. THIS IS THE MAGIC MODIFIER! ✨
            // It makes the card resize smoothly when the text inside changes size.
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                text = "Workout Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body Text
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // 4. Logic: If expanded, show ALL lines. If not, limit to 3.
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            // 5. The "Show More" Button (Only shows if text is long)
            if (isLongText) {
                Spacer(modifier = Modifier.height(8.dp))

                // Full-width clickable box for easy tapping
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isExpanded) "Show Less" else "Show More",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
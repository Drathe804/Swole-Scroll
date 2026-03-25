package com.dravenmiller.swolescroll.features.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    // 👇 NEW STATE: Controls the full-screen Battle Report replay!
    var showBattleReportReplay by remember { mutableStateOf(false) }

    val totalWorkoutVolume = remember(workout?.exercises, userWeight) {
        workout?.exercises?.sumOf { workoutExercise ->
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
                        OutlinedTextField(
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
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                Text("Loading scroll...")
            }
        } else {
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
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

                // 👇 THE GLOWING BATTLE REPORT REPLAY BUTTON!
                // This checks if your workout object actually has the new improvements list saved to it.
                // NOTE: This will error until you update your Workout.kt entity to include `val improvements: List<SkillImprovement> = emptyList()`
                if (!workout.improvements.isNullOrEmpty()) {
                    OutlinedButton(
                        onClick = { showBattleReportReplay = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD700)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)),
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "View Battle Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (!workout.notes.isNullOrBlank()) {
                    CollapsibleNoteCard(note = workout.notes, modifier = Modifier.padding(horizontal = 16.dp))
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(workout.exercises) { workoutExercise ->
                        workoutExercise.calculateTotalVolume(userWeight)
                        DetailExerciseItem(
                            workoutExercise = workoutExercise,
                            userWeight = userWeight
                        )
                    }
                }
            }
        }

        // 📊 THE FULL-SCREEN REPLAY OVERLAY
        if (showBattleReportReplay && workout != null) {
            com.dravenmiller.swolescroll.features.logworkout.BattleReportScreen(
                improvements = workout.improvements, // 👈 Passes the saved PRs!
                onBackToRewards = { showBattleReportReplay = false }, // We just hide it, no rewards screen here
                onExitQuest = { showBattleReportReplay = false }
            )
        }
    }
}

@Composable
fun CollapsibleNoteCard(
    note: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isLongText = remember(note) { note.length > 150 }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Workout Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (isLongText) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(vertical = 4.dp),
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

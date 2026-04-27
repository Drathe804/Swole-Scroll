package com.dravenmiller.swolescroll.ui.dialogs // Check your package name!

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dravenmiller.swolescroll.model.Exercise
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.ui.components.ExerciseTypeSelector
import com.dravenmiller.swolescroll.util.FreshnessUtils

@Composable
fun ExerciseSelectionDialog(
    knownExercises: List<Exercise>,
    exerciseHistory: Map<String, Long>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onUpdateExercise: (Exercise) -> Unit = { _ -> },
    // 👇 UPDATED: Accepts 5 parameters now
    onCreateNewExercise: (String, String, Boolean, ExerciseType, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddMode by remember { mutableStateOf(false) }

    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    val allMuscles = remember(knownExercises) {
        knownExercises.map { it.muscleGroup.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    if (showAddMode || exerciseToEdit != null) {
        AddEditExerciseView(
            existingExercise = exerciseToEdit,
            existingMuscleGroups = allMuscles,
            initialName = searchQuery,
            onDismiss = {
                showAddMode = false
                exerciseToEdit = null
            },
            // 👇 UPDATED: lambda now receives 5 arguments
            onSave = { name, muscle, isSingleSide, type, isBodyweight ->
                if (exerciseToEdit != null) {
                    // EDIT MODE
                    onUpdateExercise(
                        exerciseToEdit!!.copy(
                            name = name,
                            muscleGroup = muscle,
                            isSingleSide = isSingleSide,
                            type = type,
                            isBodyweight = isBodyweight // 👈 Updates existing
                        )
                    )
                } else {
                    // ADD MODE
                    onCreateNewExercise(name, muscle, isSingleSide, type, isBodyweight) // 👈 Creates new
                }
                showAddMode = false
                exerciseToEdit = null
            },
        )
    } else {
        // ... (Selection List Logic remains exactly the same) ...
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Exercise", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showAddMode = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create New Exercise")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredList = knownExercises.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.muscleGroup.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredList) { exercise ->
                            val lastDate = exerciseHistory[exercise.name]
                            val freshnessColor = FreshnessUtils.getFreshnessColor(lastDate)
                            val freshnessLabel = FreshnessUtils.getFreshnessLabel(lastDate)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExerciseSelected(exercise) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        // The Timer Icon
                                        Icon(
                                            imageVector = if (lastDate == null || lastDate == 0L) Icons.Default.AutoAwesome else Icons.Default.Timelapse, // ✨ for New, 🕒 for History
                                            contentDescription = null,
                                            tint = freshnessColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        // The Text ("3 wks" or "New Quest")
                                        Text(
                                            text = freshnessLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = freshnessColor,
                                            fontWeight = if (lastDate == null || lastDate == 0L) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(end = 4.dp) // Space between text and clock
                                        )
                                    }
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = exercise.muscleGroup,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = { exerciseToEdit = exercise }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditExerciseView(
    existingExercise: Exercise?,
    existingMuscleGroups: List<String>,
    initialName: String = "",
    onDismiss: () -> Unit,
    // 👇 UPDATED: Callback now expects 5 arguments
    onSave: (String, String, Boolean, ExerciseType, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existingExercise?.name ?: initialName) }
    val isEditing = existingExercise != null

    var muscleGroup by remember { mutableStateOf(existingExercise?.muscleGroup ?: "") }
    var isSingleSide by remember { mutableStateOf(existingExercise?.isSingleSide ?: false) }
    var selectedType by remember { mutableStateOf(existingExercise?.type ?: ExerciseType.STRENGTH) }

    // 👇 NEW STATE: Bodyweight
    var isBodyweight by remember { mutableStateOf(existingExercise?.isBodyweight ?: false) }

    val commonMuscles = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Abs", "Cardio")
    val filteredMuscles = remember(muscleGroup, existingMuscleGroups) {
        if (muscleGroup.isBlank()) emptyList()
        else existingMuscleGroups.filter {
            it.contains(muscleGroup, ignoreCase = true) &&
                    !it.equals(muscleGroup, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Edit Exercise" else "New Exercise",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text("Muscle Group") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Drop Down Suggestions
                AnimatedVisibility(visible = filteredMuscles.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            filteredMuscles.take(3).forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { muscleGroup = suggestion }
                                        .padding(vertical = 12.dp),
                                ) {
                                    Text(text = suggestion)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Common Muscle Chips
                Text("Common Groups:", style = MaterialTheme.typography.labelSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    commonMuscles.take(3).forEach { suggestion ->
                        SuggestionChip(
                            onClick = { muscleGroup = suggestion },
                            label = { Text(suggestion) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                ExerciseTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { newType ->
                        selectedType = newType
                        if (newType.isCardio) {
                            isSingleSide = false
                            isBodyweight = true // Smart default for cardio
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // 👇 NEW CHECKBOX: Bodyweight
                AnimatedVisibility(visible = !selectedType.isCardio) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBodyweight = !isBodyweight }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isBodyweight,
                            onCheckedChange = { isBodyweight = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Bodyweight Exercise?", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Adds User Weight to Total Volume",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Unilateral Checkbox
                AnimatedVisibility(visible = !selectedType.isCardio) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSingleSide = !isSingleSide }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSingleSide,
                            onCheckedChange = { isSingleSide = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Unilateral Exercise?",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (isSingleSide) "Calculates: Weight x Reps x 2" else "Calculates: Weight x Reps",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && muscleGroup.isNotBlank()) {
                                // 👇 PASS ALL 5 VALUES
                                onSave(name, muscleGroup, isSingleSide, selectedType, isBodyweight)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

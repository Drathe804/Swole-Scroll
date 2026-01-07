package com.example.swolescroll.features.logworkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swolescroll.model.Exercise
import com.example.swolescroll.model.ExerciseType
import com.example.swolescroll.ui.components.ExerciseTypeSelector
import kotlin.collections.emptyList

@Composable
fun ExerciseSelectionDialog(
    knownExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    // 👇 THIS is the parameter your screen was looking for!
    onUpdateExercise: (Exercise) -> Unit = { _ -> },
    onCreateNewExercise: (String, String, Boolean, ExerciseType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddMode by remember { mutableStateOf(false) }

    // State to hold the exercise we are currently editing
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    val allMuscles = remember(knownExercises) { knownExercises.map { it.muscleGroup.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    }

    // DECISION: Show the List OR the Edit Form?
    if (showAddMode || exerciseToEdit != null) {
        AddEditExerciseView(
            existingExercise = exerciseToEdit,
            existingMuscleGroups = allMuscles,
            initialName = searchQuery,
            onDismiss = {
                showAddMode = false
                exerciseToEdit = null
            },
            onSave = { name, muscle, isSingleSide, type ->
                if (exerciseToEdit != null) {
                    // EDIT MODE: Pass the ORIGINAL exercise object + new values
                    onUpdateExercise(exerciseToEdit!!.copy(
                        name = name,
                        muscleGroup = muscle,
                        isSingleSide = isSingleSide,
                        type = type
                    ))
                } else {
                    // ADD MODE: Create a new one
                    // onExerciseSelected(Exercise(name = name, muscleGroup = muscle))
                    onCreateNewExercise(name, muscle, isSingleSide, type)
                }
                showAddMode = false
                exerciseToEdit = null
            },
        )
    } else {
        // SELECTION LIST SCREEN
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
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

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // "Create New" Button
                    Button(
                        onClick = { showAddMode = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create New Exercise")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // The List
                    val filteredList = knownExercises.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.muscleGroup.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredList) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExerciseSelected(exercise) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(text = exercise.muscleGroup, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                // EDIT BUTTON
                                IconButton(onClick = { exerciseToEdit = exercise }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
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
    onSave: (String, String, Boolean, ExerciseType) -> Unit
) {
    // If editing, Name is locked. If new, Name is blank.
    var name by remember { mutableStateOf(existingExercise?.name ?: initialName) }
    val isEditing = existingExercise != null

    var muscleGroup by remember { mutableStateOf(existingExercise?.muscleGroup ?: "") }
    var isSingleSide by remember { mutableStateOf(existingExercise?.isSingleSide ?: false) }
    var selectedType by remember {mutableStateOf(existingExercise?.type?: ExerciseType.STRENGTH)}

    val commonMuscles = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Abs", "Cardio")
    val filteredMuscles = remember(muscleGroup, existingMuscleGroups) {
        if (muscleGroup.isBlank())emptyList()
        else existingMuscleGroups.filter {
            it.contains(muscleGroup, ignoreCase = true) &&
                    !it.equals(muscleGroup, ignoreCase = true)

        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isEditing) "Edit Exercise" else "New Exercise",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(16.dp))

                // NAME INPUT
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = true, // Grays it out if editing
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                // MUSCLE GROUP INPUT (Always Editable)
                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text("Muscle Group") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Drop Down List
                AnimatedVisibility(visible = filteredMuscles.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ){
                        Column{
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
                Text("Common Groups:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    onTypeSelected = {selectedType = it}
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSingleSide = !isSingleSide }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Checkbox(
                        checked = isSingleSide,
                        onCheckedChange = { isSingleSide = it}
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Unilateral Exercise?",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isSingleSide) "Calculates: Weight x Reps x 2" else "Calculates: Weight x Reps",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && muscleGroup.isNotBlank()) {
                                onSave(name, muscleGroup, isSingleSide, selectedType)
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

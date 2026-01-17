package com.dravenmiller.swolescroll.features.logworkout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.ui.components.EditExerciseItem
import com.dravenmiller.swolescroll.ui.components.SwoleButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutScreen(
    viewModel: LogWorkoutViewModel,
    onBackClick: () -> Unit,
    onSaveFinished: () -> Unit
) {
    val addedExercises = viewModel.addedExercises
    val knownExercises by viewModel.exerciseList.collectAsState(initial = emptyList())
    val prMapState = viewModel.personalRecords.collectAsState()
    val historyMapState = viewModel.exerciseNotesHistory.collectAsState()

    var showFinishDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // 👇 PASTE THIS HERE (Line ~65)
    var showDeleteConfirmation by remember { mutableStateOf(false) }


    var expandedIndex by remember { mutableStateOf(-1) }
    val isFocusMode = expandedIndex != -1
    var showDistanceDialog by remember { mutableStateOf(false) }
    var exerciseIdForDistance by remember { mutableStateOf("") }
    var tempTotalDistance by remember { mutableStateOf("") }

    var isEditingTitle by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<WorkoutExercise?>(null) }

    val currentSessionVolume = remember(viewModel.addedExercises.toList()) {
        viewModel.addedExercises.sumOf { workoutExercise ->
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
                    ExerciseType.TWENTY_ONES -> {
                        val rawVol = (w * set.reps * multiplier)
                        ((rawVol * 2)/3).toInt()
                    }
                    ExerciseType.CARDIO -> 0
                    else -> 0
                }
            }
        }
    }

    LaunchedEffect(addedExercises.size) {
        if (addedExercises.isNotEmpty()) {
            expandedIndex = addedExercises.lastIndex
        }
    }

    LaunchedEffect(viewModel.addedExercises.toList(), viewModel.workoutName.value, viewModel.workoutNote.value) {
        viewModel.autoSaveDraft()
    }

    if (viewModel.showDialog.value) {
        ExerciseSelectionDialog(
            knownExercises = knownExercises,
            onDismiss = { viewModel.showDialog.value = false },
            onExerciseSelected = { exercise ->
                val newEntry = WorkoutExercise(exercise = exercise, sets = emptyList())
                viewModel.addedExercises.add(newEntry)
                viewModel.showDialog.value = false
            },
            onUpdateExercise = { updatedExercise ->
                viewModel.updateExercise(updatedExercise)
            },
            onCreateNewExercise = { name, muscle, isSingleSide, ExerciseType ->
                viewModel.addExerciseSafe(name, muscle, isSingleSide, ExerciseType)
                viewModel.showDialog.value = false
            }
        )
    }

    if (viewModel.showResumeDialog.value) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Unfinished Workout Found") },
            text = { Text("Do you want to resume your unsaved workout?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resumeDraft() }) { Text("Resume") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardDraft() }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    BackHandler(enabled = true) {
        if (isFocusMode) {
            expandedIndex = -1
        } else if (viewModel.addedExercises.isNotEmpty()) {
            showExitDialog = true
        } else {
            onBackClick()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("You have unsaved progress. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardDraft()
                    showExitDialog = false
                    onBackClick()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Workout Summary") },
            text = {
                Column {
                    Text("Great job! Any notes for next time?")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.workoutNote.value,
                        onValueChange = { viewModel.workoutNote.value = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },

            confirmButton = {
                SwoleButton(
                    text = "Save & Finish",
                    onClick = {
                        showFinishDialog = false
                        viewModel.saveWorkout(onSaved = onSaveFinished)
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Cancel") }
            }
        )
    }
        // ... after if (showFinishDialog) { ... } (Line ~180)

        // 👇 PASTE THIS HERE
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Workout?") },
                text = { Text("This action cannot be undone. Are you sure?") },
                confirmButton = {
                    Button(
                        onClick = {
                            // We reuse onSaveFinished to exit the screen after deleting
                            viewModel.deleteCurrentWorkout(onDeleted = onSaveFinished)
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
                }
            )
        }

    Scaffold(
        topBar = {
            if (!isFocusMode) {
                val displayTitle = when {
                    viewModel.workoutName.value.isNotBlank() -> viewModel.workoutName.value
                    viewModel.addedExercises.isNotEmpty() -> "${viewModel.addedExercises.first().exercise.muscleGroup} Day"
                    else -> "New Entry"
                }

                TopAppBar(
                    title = {
                        Text(
                            text = displayTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { isEditingTitle = true }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        val title = if (expandedIndex in addedExercises.indices) {
                            addedExercises[expandedIndex].exercise.name
                        } else ""
                        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = { expandedIndex = -1 }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Focus")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .imePadding()
        ) {
            AnimatedVisibility(visible = !isFocusMode) {
                Column {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = viewModel.workoutDate.value
                    )
                    var showDatePicker by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Date")
                        Spacer(Modifier.width(8.dp))
                        val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(viewModel.workoutDate.value))
                        Text(text = "Date: $dateString")
                    }

                    if (showDatePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    datePickerState.selectedDateMillis?.let {
                                        viewModel.workoutDate.value = it
                                    }
                                    showDatePicker = false
                                }) { Text("OK") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentSessionVolume > 0){
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ){
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Session Volume: ${java.text.NumberFormat.getIntegerInstance().format(currentSessionVolume)} lbs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    if (isEditingTitle) {
                        var localName by remember { mutableStateOf(viewModel.workoutName.value) }
                        OutlinedTextField(
                            value = localName,
                            onValueChange = { localName = it },
                            placeholder = { Text("e.g., Chest Destruction") },
                            label = { Text("Workout Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                viewModel.workoutName.value = localName
                                isEditingTitle = false
                            }),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.workoutName.value = localName
                                    isEditingTitle = false
                                }){
                                    Icon(Icons.Default.Check, contentDescription = "Save Name")
                                }
                            }
                        )
                    } else {
                        val smartName = when {
                            viewModel.workoutName.value.isNotBlank() -> viewModel.workoutName.value
                            viewModel.addedExercises.isNotEmpty() -> "${viewModel.addedExercises.first().exercise.muscleGroup} Day"
                            else -> "Tap to name workout"
                        }
                        Text(
                            text = smartName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEditingTitle = true }
                                .padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (addedExercises.isEmpty()) {
                    item { Text("No exercises. Add one to start!", modifier = Modifier.padding(top = 16.dp)) }
                } else {
                    items(addedExercises.size) { index ->
                        if (!isFocusMode || expandedIndex == index) {
                            val workoutExercise = addedExercises[index]
                            val thisPr = prMapState.value[workoutExercise.exercise.name]
                            val thisHistory = historyMapState.value[workoutExercise.exercise.name] ?: emptyList()
                            val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                            // get historical pr
                            val historyPrString = prMapState.value[workoutExercise.exercise.name]
                            val currentBestValue = remember(workoutExercise.sets, workoutExercise.exercise.type){
                                when (type) {
                                    ExerciseType.CARDIO -> workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                    ExerciseType.LoadedCarry -> workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
                                    else -> workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
                                }
                            }
                            val historyValue = remember(historyPrString){
                                historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                            }
                            val bestSetToday = workoutExercise.sets.maxByOrNull { it.weight }
                            val currentBestWeight = bestSetToday?.weight ?: 0.0
                            val currentBestReps = bestSetToday?.reps ?: 0
                            val currentBestDistance = bestSetToday?.distance ?: 0.0
                            val currentBestTime = bestSetToday?.time ?: 0

                            val isValidSet = when (type) {
                                ExerciseType.CARDIO -> currentBestValue > 0 // Distance > 0
                                ExerciseType.LoadedCarry -> currentBestDistance > 0
                                ExerciseType.ISOMETRIC -> currentBestTime > 0
                                else -> currentBestReps > 0 // Strength/21s needs reps
                            }

                            var displayPr = historyPrString
                            var isNewRecord = false
                            if (historyValue > 0 && currentBestWeight > historyValue && isValidSet){
                                isNewRecord = true
                                displayPr = when (type){
                                    ExerciseType.CARDIO -> {
                                        // For Cardio, we sum everything, so we don't use 'bestSetToday'
                                        val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                        val cleanDist = String.format("%.2f", totalDist).removeSuffix("0").removeSuffix("0").removeSuffix(".")
                                        "$cleanDist ${if(workoutExercise.exercise.name.contains("Stair")) "stairs" else "mi"}"
                                    }
                                    ExerciseType.LoadedCarry -> {
                                        val dist = bestSetToday?.distance ?: 0.0
                                        // Optional: Clean up "50.0" to "50"
                                        val cleanDist = dist.toString().removeSuffix(".0")
                                        "$currentBestWeight lbs for $cleanDist yds"
                                    }
                                    ExerciseType.ISOMETRIC -> "$currentBestWeight lbs"

                                    // STRENGTH & 21s: Show "Weight x Reps"
                                    else -> "$currentBestWeight lbs x $currentBestReps"
                                }
                            }

                            Column(modifier = Modifier.animateContentSize()) {
                                EditExerciseItem(
                                    workoutExercise = workoutExercise,
                                    isExpanded = expandedIndex == index,
                                    personalRecord = displayPr,
                                    isNewPr = isNewRecord,
                                    pastNotes = thisHistory,
                                    onDelete = {
                                        if (workoutExercise.sets.isEmpty()){
                                            viewModel.addedExercises.remove(workoutExercise)
                                        } else {
                                            exerciseToDelete = workoutExercise
                                        }
                                    },
                                    onInfoClick = {
                                        viewModel.loadHistory(workoutExercise.exercise.id, workoutExercise.exercise.name)
                                        viewModel.showHistoryDialog.value = true
                                    },
                                    onHeaderClick = {
                                        expandedIndex = if (expandedIndex == index) -1 else index
                                    },
                                    onAddSet = {
                                        val newSet = Set(weight = 0.0, reps = 0)
                                        val updatedExercise = workoutExercise.copy(sets = workoutExercise.sets + newSet)
                                        viewModel.addedExercises[index] = updatedExercise
                                    },
                                    onUpdateSet = { setIndex, updatedSet ->
                                        val updatedSets = workoutExercise.sets.toMutableList()
                                        updatedSets[setIndex] = updatedSet
                                        val updatedExercise = workoutExercise.copy(sets = updatedSets)
                                        viewModel.addedExercises[index] = updatedExercise
                                    },
                                    onRemoveSet = { setIndex ->
                                        val updatedSets = workoutExercise.sets.toMutableList()
                                        updatedSets.removeAt(setIndex)
                                        val updatedExercise = workoutExercise.copy(sets = updatedSets)
                                        viewModel.addedExercises[index] = updatedExercise
                                    },
                                    onUpdateNote = { newNote ->
                                        val updatedExercise = workoutExercise.copy(note = newNote)
                                        viewModel.addedExercises[index] = updatedExercise
                                    },
                                    onTreadmillSplit = { seconds, incline, level ->
                                        // 1. UPDATE THE VIEWMODEL
                                        viewModel.splitCardioSet(
                                            exerciseId = workoutExercise.id,
                                            currentSetIndex = workoutExercise.sets.lastIndex,
                                            elapsedSeconds = seconds,
                                            newIncline = incline,
                                            newLevel = level
                                        )

                                        // 2. CHECK IF WE SHOULD SHOW THE POPUP 🧠
                                        // Treadmill: incline = Weight (Speed), level = Reps (Inc)
                                        val isTreadmillCheck = workoutExercise.exercise.name.contains("Treadmill", ignoreCase = true)

                                        // ✅ FIXED LOGIC HERE:
                                        // If Treadmill: Stop when Incline (which holds Speed) == 0.0
                                        // If Bike/Stairs: Stop when Level (which holds Weight) == 0.0 (Wait, level passed here is Int Reps)

                                        // Actually, let's look at what 'incline' and 'level' are passed from EditExerciseItem:
                                        // EditExerciseItem calls this as: onTreadmillSplit(seconds, primaryValue, secondaryValue)
                                        // primaryValue = Level (Speed/Weight) -> "incline" arg here
                                        // secondaryValue = Incline (Reps) -> "level" arg here

                                        // So 'incline' arg IS the Speed (Weight).
                                        // And 'level' arg IS the Incline (Reps).
                                        // Confusing naming in the lambda, but the logic should be:

                                        val speedOrLevel = incline // This is the Primary Value (Weight)

                                        // We only stop if Speed drops to 0.
                                        if (speedOrLevel == 0.0 && workoutExercise.sets.isNotEmpty()){
                                            exerciseIdForDistance = workoutExercise.id
                                            tempTotalDistance = ""
                                            showDistanceDialog = true
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isFocusMode) {

                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    SwoleButton(text = "Add Exercise", onClick = { viewModel.showDialog.value = true })
                    Spacer(modifier = Modifier.height(8.dp))
                    SwoleButton(text = "Finish Workout", onClick = { showFinishDialog = true })

                    // 👇 PASTE THIS HERE (Line ~530)
                    // 🗑️ DELETE BUTTON (Only visible if Editing)
                    if (viewModel.currentWorkoutId != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Entire Workout")
                        }
                    }
                }

            }

            // REPLACE THE OLD "Done Editing" BUTTON WITH NAVIGATION ROW 👇
            AnimatedVisibility(visible = isFocusMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⬆️ PREVIOUS EXERCISE (Up in the list)
                    androidx.compose.material3.FilledTonalIconButton(
                        onClick = {
                            if (expandedIndex > 0) {
                                val target = expandedIndex - 1
                                viewModel.prepareForSuperset(target)
                                expandedIndex = target
                            }
                        },
                        enabled = expandedIndex > 0, // Disable if at top
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Previous Exercise",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // ❌ CLOSE FOCUS
                    androidx.compose.material3.OutlinedButton(
                        onClick = { expandedIndex = -1 },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Done")
                    }

                    // ⬇️ NEXT EXERCISE (Down in the list)
                    androidx.compose.material3.FilledTonalIconButton(
                        onClick = {
                            if (expandedIndex < addedExercises.lastIndex) {
                                val target = expandedIndex + 1
                                viewModel.prepareForSuperset(target)
                                expandedIndex = target
                            }
                        },
                        enabled = expandedIndex < addedExercises.lastIndex, // Disable if at bottom
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Next Exercise",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            if (viewModel.showHistoryDialog.value) {
                val history by viewModel.exerciseHistory.collectAsState()
                ExerciseHistoryDialog(
                    exerciseName = viewModel.historyTitle,
                    history = history,
                    onDismiss = { viewModel.showHistoryDialog.value = false }
                )
            }
            if (exerciseToDelete != null) {
                AlertDialog(
                    onDismissRequest = { exerciseToDelete = null },
                    title = { Text("Remove Exercise?") },
                    text = { Text("You have logged sets for this exercise. Are you sure you want to remove this exercise?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addedExercises.remove(exerciseToDelete)
                            exerciseToDelete = null
                        }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { exerciseToDelete = null }) { Text("Cancel") }
                    }
                )
            }
            if (showDistanceDialog) {
                AlertDialog(
                    onDismissRequest = { showDistanceDialog = false },
                    title = { Text("Workout Paused") },
                    text = {
                        Column {
                            Text("Enter total distance shown on machine:")
                            OutlinedTextField(
                                value = tempTotalDistance,
                                onValueChange = {
                                    if (it.count { char -> char == '.' } <= 1 && it.all { char -> char.isDigit() || char == '.' }) {
                                        tempTotalDistance = it
                                    }
                                },
                                placeholder = { Text("e.g. 3.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val dist = tempTotalDistance.toDoubleOrNull() ?: 0.0
                            if (dist > 0) {
                                viewModel.applyDistanceToExercise(exerciseIdForDistance, dist)
                            }
                            showDistanceDialog = false
                        }) { Text("Calculate & Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDistanceDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

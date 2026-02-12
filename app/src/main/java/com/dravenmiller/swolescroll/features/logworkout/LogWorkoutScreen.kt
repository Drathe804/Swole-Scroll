package com.dravenmiller.swolescroll.features.logworkout

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.ui.components.EditExerciseItem
import com.dravenmiller.swolescroll.ui.components.SwoleButton
import com.dravenmiller.swolescroll.ui.dialogs.ExerciseSelectionDialog
import com.dravenmiller.swolescroll.util.BodyweightMath
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Map.entry
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.Q)
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
    val userWeight by viewModel.userBodyWeight.collectAsState()
    val freshnessMap by viewModel.exerciseFreshnessMap.collectAsState()

    var isEditingTitle by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<WorkoutExercise?>(null) }

    val currentSessionVolume = remember(viewModel.addedExercises.toList()) {
        viewModel.addedExercises.sumOf { workoutExercise ->
            workoutExercise.sets.sumOf { set ->
                val multiplier = if (workoutExercise.exercise.isSingleSide) 2 else 1
                val bwPercentage = BodyweightMath.getMultiplier(workoutExercise.exercise.name)
                val w = if (workoutExercise.exercise.isBodyweight) {
                    (userWeight * bwPercentage) + set.weight
                } else {
                    set.weight
                }
                val d = set.distance ?: 0.0
                val t = set.time ?: 0
                val safeType = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                when (safeType) {
                    ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                    ExerciseType.ISOMETRIC -> 0
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

    val listState = rememberLazyListState()


    LaunchedEffect(viewModel.addedExercises.toList(), viewModel.workoutName.value, viewModel.workoutNote.value) {
        viewModel.autoSaveDraft()
    }

    if (viewModel.showDialog.value) {
        ExerciseSelectionDialog(
            knownExercises = knownExercises,
            exerciseHistory = freshnessMap,
            onDismiss = {
                viewModel.showDialog.value = false
                viewModel.activeDungeonId = null
            },
            onExerciseSelected = { exercise ->
                val initialSet = Set(
                    id = UUID.randomUUID().toString(),
                    weight = 0.0,
                    reps = 0,
                    distance = 0.0,
                    time = 0
                )
                if (viewModel.activeDungeonId != null) {
                    // ⚔️ DUNGEON MODE: Add to the specific group
                    val newEntry = WorkoutExercise(
                        id = UUID.randomUUID().toString(),
                        exercise = exercise,
                        sets = listOf(initialSet),
                        supersetId = viewModel.activeDungeonId // 👈 TAG THE MINION
                    )

                    // Insert it right after the current focus
                    viewModel.addedExercises.add(expandedIndex + 1, newEntry)

                    // Move focus to the new guy
                    expandedIndex++
                    viewModel.showDialog.value = false

                    // Reset ID (or keep it if you want to add multiple minions)
                    viewModel.activeDungeonId = null
                } else {
                    val newEntry = WorkoutExercise(exercise = exercise, sets = listOf(initialSet))
                    viewModel.addedExercises.add(newEntry)
                    // Explicitly Focus the NEW item (which is now at the end)
                    expandedIndex = viewModel.addedExercises.lastIndex
                    viewModel.showDialog.value = false
                }
            },
            onUpdateExercise = { updatedExercise ->
                viewModel.updateExercise(updatedExercise)
            },
            onCreateNewExercise = { name, muscle, isSingleSide, type, isBodyweight ->
                viewModel.addExerciseSafe(
                    name,
                    muscle,
                    isSingleSide,
                    type,
                    isBodyweight
                )
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
                                    text = "Session Volume: ${NumberFormat.getIntegerInstance().format(currentSessionVolume)} lbs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Check if it's a quest AND has instructions
                    if (viewModel.isQuest.value && viewModel.workoutNote.value.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer, // Distinct "Quest" color
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null) // ⚔️ or ✨
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Mission Briefing",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = viewModel.workoutNote.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic
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
                    }


                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
                }
            }

            LazyColumn(
                state = listState,
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
                            val isDungeon = workoutExercise.supersetId != null

                            // 1. GET HISTORY (Parse Weight AND Reps) 🕵️‍♂️
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

                            val historyWeight = remember(historyPrString) {
                                historyPrString?.split("x")?.firstOrNull()
                                    ?.replace("lbs", "")?.trim()?.toDoubleOrNull() ?: 0.0
                            }

                            val historyReps = remember(historyPrString) {
                                // Looks for the number after "x" (e.g., "150 lbs x 5")
                                historyPrString?.split("x")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                            }
                            val isWeightPR = currentBestWeight > historyWeight
                            val isRepPR = (currentBestWeight == historyWeight) && (currentBestWeight > 0) && (currentBestReps > historyReps)

                            // 2. CALCULATE "IS NEW RECORD" BASED ON TYPE 🧠
                            val isNewRecord = remember(workoutExercise.sets, historyPrString, type) {
                                when (type) {
                                    ExerciseType.CARDIO -> {
                                        // 🏃 SPEED CHECK
                                        val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                        val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }

                                        if (totalDist > 0 && totalSeconds > 0) {
                                            val isStairs = workoutExercise.exercise.name.contains("stairs", ignoreCase = true)

                                            // 1. Calculate Current Speed
                                            val currentSpeed = if (isStairs){
                                                val minutes = totalSeconds / 60.0
                                                if (minutes > 0) totalDist / minutes else 0.0
                                            } else {
                                                val hours = totalSeconds / 3600.0
                                                if (hours > 0) totalDist / hours else 0.0 // mph
                                            }

                                            // 2. Extract History (Handle "Bad Data" Fix) 🛠️
                                            val rawHistory = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0

                                            // If it's Stairs but the history says "mph", it's the old "Stairs per Hour" math.
                                            // We divide by 60 to convert it to "Stairs per Minute".
                                            val historySpeed = if (isStairs && historyPrString?.contains("mph") == true) {
                                                rawHistory / 60.0
                                            } else {
                                                rawHistory
                                            }

                                            currentSpeed > historySpeed
                                        } else {
                                            false
                                        }
                                    }

                                    ExerciseType.LoadedCarry -> {
                                        // 🏋️ CARRY CHECK: Heavier OR (Same Weight + Further)
                                        val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
                                        val currentW = bestSet?.weight ?: 0.0
                                        val currentD = bestSet?.distance ?: 0.0

                                        // History string: "100.0 lbs for 50.0 yds"
                                        val parts = historyPrString?.split(" ")
                                        val historyW = parts?.firstOrNull()?.toDoubleOrNull() ?: 0.0 // "100.0"
                                        // "for" is index 2, dist is index 3? Let's be safer: look for "yds" predecessor
                                        val historyD = historyPrString?.substringAfter("for ")?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0

                                        if (currentD > 0){
                                            val isHeavier = currentW > historyW
                                            val isFurther = (currentW == historyW) && (currentW > 0) && (currentD > historyD)
                                            isHeavier || isFurther
                                        } else {
                                            false
                                        }
                                    }
                                    ExerciseType.ISOMETRIC -> {
                                        // Heavier Hold
                                        val maxWeight = workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
                                        val maxTime = workoutExercise.sets.maxOfOrNull { it.time } ?: 0
                                        val historyWeight = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                                        maxWeight > historyWeight && maxWeight > 0 && maxTime > 0
                                    }
                                    else -> {
                                        // STRENGTH (Weight x Reps)
                                        val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
                                        val w = bestSet?.weight ?: 0.0
                                        val r = bestSet?.reps ?: 0

                                        val histWeight = historyPrString?.split("x")?.firstOrNull()?.replace("lbs", "")?.trim()?.toDoubleOrNull() ?: 0.0
                                        val histReps = historyPrString?.split("x")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

                                        if(r > 0) {
                                            val isWeightPR = w > histWeight
                                            val isRepPR =
                                                (w == histWeight) && (w > 0) && (r > histReps)

                                            isWeightPR || isRepPR
                                        } else {
                                            false
                                        }
                                    }
                                }
                            }

                            // FORMAT DISPLAY TEXT
                            var displayPr = historyPrString

                            // 🆕 FIX LEGACY DATA DISPLAY 🧹
                            // If we have an old "mph" record for Stairs, fix the text immediately
                            if (type == ExerciseType.CARDIO &&
                                workoutExercise.exercise.name.contains("stairs", ignoreCase = true) &&
                                historyPrString?.contains("mph") == true) {

                                val raw = historyPrString.split(" ").firstOrNull()?.toDoubleOrNull() ?: 0.0
                                val fixed = raw / 60.0
                                displayPr = "${String.format("%.2f", fixed)} stairs/min"
                            }

                            if (isNewRecord) {
                                displayPr = when (type){
                                    ExerciseType.CARDIO -> {
                                        val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                        val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }
                                        val isStairs = workoutExercise.exercise.name.contains("stairs", ignoreCase = true)

                                        if (isStairs) {
                                            // 🪜 STAIRS TEXT
                                            val minutes = totalSeconds / 60.0
                                            val spm = if (minutes > 0) totalDist / minutes else 0.0
                                            "${String.format("%.2f", spm)} stairs/min"
                                        } else {
                                            // 🏃 RUNNING TEXT
                                            val hours = totalSeconds / 3600.0
                                            val speed = if (hours > 0) totalDist / hours else 0.0
                                            "${String.format("%.2f", speed)} mph"
                                        }
                                    }
                                    ExerciseType.LoadedCarry -> {
                                        val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
                                        "${bestSet?.weight} lbs for ${bestSet?.distance} yds"
                                    }
                                    // ... others same as before ...
                                    else -> "$currentBestWeight lbs x $currentBestReps"
                                }
                            }


                            Column(modifier = Modifier.animateContentSize()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        // Darker color for Dungeon, Standard for Normal
                                        containerColor = if (isDungeon) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Column {
                                        // Optional: A little header to show they are linked
                                        if (isDungeon) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Link,
                                                    contentDescription = "Linked",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Mini-Dungeon",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        EditExerciseItem(
                                            workoutExercise = workoutExercise,
                                            userWeight = userWeight,
                                            isExpanded = expandedIndex == index,
                                            personalRecord = displayPr,
                                            isNewPr = isNewRecord,
                                            pastNotes = thisHistory,
                                            onDelete = {
                                                if (workoutExercise.sets.isEmpty()) {
                                                    viewModel.addedExercises.remove(workoutExercise)
                                                    expandedIndex = -1
                                                } else {
                                                    exerciseToDelete = workoutExercise
                                                }
                                            },
                                            onInfoClick = {
                                                viewModel.loadHistory(
                                                    workoutExercise.exercise.id,
                                                    workoutExercise.exercise.name
                                                )
                                                viewModel.showHistoryDialog.value = true
                                            },
                                            onHeaderClick = {
                                                expandedIndex =
                                                    if (expandedIndex == index) -1 else index
                                            },
                                            onAddSet = {
                                                val newSet = Set(weight = 0.0, reps = 0)
                                                val updatedExercise =
                                                    workoutExercise.copy(sets = workoutExercise.sets + newSet)
                                                viewModel.addedExercises[index] = updatedExercise
                                            },
                                            onUpdateSet = { setIndex, updatedSet ->
                                                val updatedSets =
                                                    workoutExercise.sets.toMutableList()
                                                updatedSets[setIndex] = updatedSet
                                                val updatedExercise =
                                                    workoutExercise.copy(sets = updatedSets)
                                                viewModel.addedExercises[index] = updatedExercise
                                            },
                                            onRemoveSet = { setIndex ->
                                                val updatedSets =
                                                    workoutExercise.sets.toMutableList()
                                                updatedSets.removeAt(setIndex)
                                                val updatedExercise =
                                                    workoutExercise.copy(sets = updatedSets)
                                                viewModel.addedExercises[index] = updatedExercise
                                            },
                                            onUpdateNote = { newNote ->
                                                val updatedExercise =
                                                    workoutExercise.copy(note = newNote)
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
                                                val isTreadmillCheck =
                                                    workoutExercise.exercise.name.contains(
                                                        "Treadmill",
                                                        ignoreCase = true
                                                    )

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

                                                val speedOrLevel =
                                                    incline // This is the Primary Value (Weight)

                                                // We only stop if Speed drops to 0.
                                                if (speedOrLevel == 0.0 && workoutExercise.sets.isNotEmpty()) {
                                                    exerciseIdForDistance = workoutExercise.id
                                                    tempTotalDistance = ""
                                                    showDistanceDialog = true
                                                }
                                            },
                                            backgroundColor = if (isDungeon) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !isFocusMode) {

                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    SwoleButton(
                        text = "Add Exercise",
                        onClick = {
                            viewModel.activeDungeonId = null
                            viewModel.showDialog.value = true
                        })
                    Spacer(modifier = Modifier.height(8.dp))
                    SwoleButton(text = "Finish Workout", onClick = { showFinishDialog = true })

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

            // 👇 SMART NAVIGATION & MINI-DUNGEON
            AnimatedVisibility(visible = isFocusMode) {
                // 1. Wrap in Column so Dungeon Button sits ABOVE Navigation
                Column {

                    // --- SAFETY CHECKS (Fixes the Crash) ---
                    // We use getOrNull so if index is -1 (during animation), it returns null instead of crashing
                    val currentExercise = addedExercises.getOrNull(expandedIndex)
                    val currentDungeonId = currentExercise?.supersetId

                    // Check if the NEXT exercise is already a superset (so we don't double add)
                    val hasNeighbor = if (expandedIndex + 1 < addedExercises.size) {
                        addedExercises[expandedIndex + 1].supersetId == currentDungeonId && currentDungeonId != null
                    } else false

                    // --- ⚔️ MINI-DUNGEON CONTROLS ---
                    if (currentExercise != null && !hasNeighbor) {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {

                            // BUTTON 1: EXTEND / CONVERT (The main action)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (currentDungeonId != null) {
                                        // 🔗 EXTEND: Add another minion to THIS dungeon
                                        viewModel.activeDungeonId = currentDungeonId
                                        viewModel.showDialog.value = true
                                    } else {
                                        // 👑 CONVERT: Turn this Normal card into a Boss
                                        viewModel.startDungeon(expandedIndex)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Castle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (currentDungeonId == null) "Enter Mini-Dungeon (Create Superset)" else "New Encounter (Add to Superset)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // BUTTON 2: BREAK CHAIN (Only show if we are already inside a dungeon)
                            if (currentDungeonId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.startNewDungeon_Fresh() }, // 👈 Requires the new VM function below
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Castle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Mini-Dungeon (Separate Superset)")
                                }
                            }
                        }
                    }


                    // 1. Calculate Names
                    val prevExerciseName = if (expandedIndex > 0) {
                        addedExercises[expandedIndex - 1].exercise.name
                    } else null

                    val nextExerciseName = if (expandedIndex < addedExercises.lastIndex) {
                        addedExercises[expandedIndex + 1].exercise.name
                    } else "Add New"

                    // 2. Check End of Dungeon Logic
                    val isEndOfDungeon = if (currentDungeonId != null) {
                        val nextIndex = expandedIndex + 1
                        nextIndex >= addedExercises.size || addedExercises[nextIndex].supersetId != currentDungeonId
                    } else false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ⬅️ PREV BUTTON
                        OutlinedButton(
                            onClick = {
                                if (expandedIndex > 0) {
                                    val target = expandedIndex - 1
                                    viewModel.prepareForSuperset(target)
                                    expandedIndex = target
                                }
                            },
                            enabled = expandedIndex > 0,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(20.dp))
                                if (prevExerciseName != null) {
                                    Text(
                                        text = prevExerciseName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // ➡️ NEXT / LOOP BUTTONS
                        if (isEndOfDungeon) {
                            // 🚪 ESCAPE
                            OutlinedButton(
                                onClick = {
                                    if (expandedIndex + 1 < addedExercises.size) {
                                        expandedIndex++
                                    } else {
                                        viewModel.showDialog.value = true
                                    }
                                },
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Escape", maxLines = 1, fontSize = 10.sp)
                            }

                            Spacer(Modifier.width(8.dp))

                            // 🔄 LOOP
                            Button(
                                onClick = {
                                    val startOfDungeon = addedExercises.indexOfFirst { it.supersetId == currentDungeonId }
                                    if (startOfDungeon != -1) {
                                        viewModel.prepareForSuperset(startOfDungeon)
                                        expandedIndex = startOfDungeon
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Superset")
                            }

                        } else {
                            // STANDARD NEXT
                            Button(
                                onClick = {
                                    if (expandedIndex < addedExercises.lastIndex) {
                                        val target = expandedIndex + 1
                                        viewModel.prepareForSuperset(target)
                                        expandedIndex = target
                                    } else {
                                        viewModel.showDialog.value = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (expandedIndex == addedExercises.lastIndex) Icons.Default.Add else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = nextExerciseName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
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
                            expandedIndex = -1
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

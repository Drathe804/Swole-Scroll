package com.dravenmiller.swolescroll.features.logworkout

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.ui.components.ActiveExerciseCard
import com.dravenmiller.swolescroll.ui.components.HordeSiegeBanner
import com.dravenmiller.swolescroll.ui.components.MiniDungeonControls
import com.dravenmiller.swolescroll.model.SkillImprovement
import com.dravenmiller.swolescroll.ui.components.SwoleButton
import com.dravenmiller.swolescroll.ui.components.VictoryOverlay
import com.dravenmiller.swolescroll.ui.dialogs.DeleteWorkoutDialog
import com.dravenmiller.swolescroll.ui.dialogs.DistanceEntryDialog
import com.dravenmiller.swolescroll.ui.dialogs.ExerciseSelectionDialog
import com.dravenmiller.swolescroll.ui.dialogs.ExitWorkoutDialog
import com.dravenmiller.swolescroll.ui.dialogs.FinishWorkoutDialog
import com.dravenmiller.swolescroll.ui.dialogs.ResumeWorkoutDialog
import com.dravenmiller.swolescroll.util.BodyweightMath
import com.dravenmiller.swolescroll.util.MonsterRoster
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val monsterHpMap by viewModel.monsterHpMap.collectAsState()
    val historicalHordeHpMap by viewModel.historicalHordeHpMap.collectAsState()
    val knownExercises by viewModel.exerciseList.collectAsState(initial = emptyList())
    val prMapState = viewModel.personalRecords.collectAsState()
    val historyMapState = viewModel.exerciseNotesHistory.collectAsState()

    val isSiegeModeEnabled by viewModel.isSiegeModeEnabled.collectAsState()
    var isArenaExpanded by remember { mutableStateOf(true) }
    var showQuestDetails by remember { mutableStateOf(true) }

    val lifetimeVolume by viewModel.lifetimeVolume.collectAsState()

    // Victory Screen State
    var showVictoryOverlay by remember { mutableStateOf(false) }
    var capturedStartXp by remember { mutableStateOf(0) }
    var capturedGainedXp by remember { mutableStateOf(0) }
    var capturedImprovements by remember { mutableStateOf<List<SkillImprovement>>(emptyList()) }
    var showBattleReportScreen by remember { mutableStateOf(false) }
    // 🧠 THE SHARED BRAIN
    val workoutDominantMuscle = remember(addedExercises.firstOrNull()?.exercise?.muscleGroup) {
        val raw = addedExercises.firstOrNull()?.exercise?.muscleGroup ?: "Chest"
        viewModel.getBroadMuscleGroup(raw)
    }

    val activeHordeLineup = remember(workoutDominantMuscle) {
        MonsterRoster.getHordeLineup(workoutDominantMuscle)
    }

    var capturedLastWeekHp by remember { mutableStateOf(0) }


    var showFinishDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
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
                set.time ?: 0
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
                    val newEntry = WorkoutExercise(
                        id = UUID.randomUUID().toString(),
                        exercise = exercise,
                        sets = listOf(initialSet),
                        supersetId = viewModel.activeDungeonId
                    )

                    viewModel.addedExercises.add(expandedIndex + 1, newEntry)
                    expandedIndex++
                    viewModel.showDialog.value = false
                    viewModel.activeDungeonId = null
                } else {
                    val newEntry = WorkoutExercise(exercise = exercise, sets = listOf(initialSet))
                    viewModel.addedExercises.add(newEntry)
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

    // --- 💬 WORKOUT DIALOGS ---

    if (viewModel.showResumeDialog.value) {
        ResumeWorkoutDialog(
            onResume = { viewModel.resumeDraft() },
            onDiscard = { viewModel.discardDraft() }
        )
    }

    if (showExitDialog) {
        ExitWorkoutDialog(
            onSaveAndExit = {
                viewModel.autoSaveDraft()
                showExitDialog = false
                onBackClick()
            },
            onDiscard = {
                viewModel.discardDraft()
                showExitDialog = false
                onBackClick()
            },
            onCancel = { showExitDialog = false }
        )
    }

    if (showFinishDialog) {
        FinishWorkoutDialog(
            workoutNote = viewModel.workoutNote.value,
            onNoteChange = { viewModel.workoutNote.value = it },
            onSaveAndFinish = {
                capturedImprovements = calculateSessionImprovements(viewModel.addedExercises, prMapState.value)

                // 👇 Capture the HP to pass to the Victory Screen
                val rawMuscle = addedExercises.firstOrNull()?.exercise?.muscleGroup ?: ""
                val domMuscle = viewModel.getBroadMuscleGroup(rawMuscle)
                capturedLastWeekHp = historicalHordeHpMap[domMuscle] ?: 5000

                if (isSiegeModeEnabled && currentSessionVolume > 0) {
                    capturedStartXp = lifetimeVolume
                    capturedGainedXp = currentSessionVolume
                    showFinishDialog = false
                    showVictoryOverlay = true
                    showFinishDialog = false
                } else {
                    viewModel.saveWorkout(improvements = capturedImprovements, onSaved = onSaveFinished)
                    showFinishDialog = false
                }
            },
            onCancel = { showFinishDialog = false }
        )
    }

    if (showDeleteConfirmation) {
        DeleteWorkoutDialog(
            onDelete = {
                viewModel.deleteCurrentWorkout(onDeleted = onSaveFinished)
                showDeleteConfirmation = false
            },
            onCancel = { showDeleteConfirmation = false }
        )
    }


    // 🧠 TOP APP BAR HUD MATH (Calculates Monster HP for the Top Menu!)
    val currentFocusedExercise = addedExercises.getOrNull(expandedIndex)
    val focusedType = currentFocusedExercise?.exercise?.type ?: ExerciseType.STRENGTH
    val showHudInTopBar = isSiegeModeEnabled && isFocusMode && !focusedType.isCardio && currentFocusedExercise != null

    Scaffold(
        topBar = {
            if (!isFocusMode) {
                val displayTitle = when {
                    viewModel.workoutName.value.isNotBlank() -> viewModel.workoutName.value
                    viewModel.addedExercises.isNotEmpty() -> {
                        // 👇 Runs the first exercise through your Faction Sorter!
                        val rawMuscle = viewModel.addedExercises.first().exercise.muscleGroup
                        val broadMuscle = viewModel.getBroadMuscleGroup(rawMuscle)
                        "$broadMuscle Day"
                    }
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
                    // 👇 THE NEW INFO TOGGLE BUTTON
                    actions = {
                        IconButton(onClick = { showQuestDetails = !showQuestDetails }) {
                            Icon(
                                imageVector = if (showQuestDetails) Icons.Default.KeyboardArrowUp else Icons.Default.Info,
                                contentDescription = "Quest Info"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // ⚔️ FOCUS MODE TOP APP BAR (The Monster HUD!)
                TopAppBar(
                    title = {
                        if (showHudInTopBar && currentFocusedExercise != null) {
                            // Calculate Live Damage
                            val bwMultiplier = BodyweightMath.getMultiplier(currentFocusedExercise.exercise.name)
                            val currentVol = currentFocusedExercise.sets.sumOf { set ->
                                val multiplier = if (currentFocusedExercise.exercise.isSingleSide) 2 else 1
                                val safeWeight = if (currentFocusedExercise.exercise.isBodyweight) (userWeight * bwMultiplier) + set.weight else set.weight
                                val safeDist = set.distance ?: 0.0
                                val safeTime = set.time ?: 0
                                when (focusedType) {
                                    ExerciseType.STRENGTH -> (safeWeight * set.reps * multiplier).toInt()
                                    ExerciseType.ISOMETRIC -> (safeWeight * safeTime * multiplier).toInt()
                                    ExerciseType.LoadedCarry -> (safeWeight * safeDist * multiplier).toInt()
                                    ExerciseType.TWENTY_ONES -> (((safeWeight * set.reps * multiplier) * 2) / 3).toInt()
                                    else -> 0
                                }
                            }

                            val isPhoenix = currentFocusedExercise.exercise.name.contains("Phoenix", ignoreCase = true)
                            // 👇 Reads directly from the Shared Brain list!
                            val img = if (isPhoenix) {
                                com.dravenmiller.swolescroll.R.drawable.monster_phoenix
                            } else {
                                if (activeHordeLineup.isNotEmpty()) {
                                    // Pulls the exact monster standing at this index in the banner
                                    activeHordeLineup[expandedIndex % activeHordeLineup.size]
                                } else {
                                    com.dravenmiller.swolescroll.R.drawable.ic_launcher_foreground
                                }
                            }

                            val maxHp = monsterHpMap[currentFocusedExercise.exercise.name] ?: 2000
                            val remHp = (maxHp - currentVol).coerceAtLeast(0)
                            val pct = (remHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

                            // Render The Monster directly in the Top Bar!
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 12.dp)) {
                                Image(
                                    painter = painterResource(img),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentFocusedExercise.exercise.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        strokeCap = StrokeCap.Round
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("HP: $remHp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        Text("DMG: $currentVol", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Standard Title (Cardio / Normal Mode)
                            val title = if (expandedIndex in addedExercises.indices) {
                                addedExercises[expandedIndex].exercise.name
                            } else ""
                            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
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
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            AnimatedVisibility(visible = !isFocusMode) {
                Column {
                    // 🗺️ QUEST DETAILS FOLDER (Toggled by the TopAppBar Info Button!)
                    androidx.compose.animation.AnimatedVisibility(visible = showQuestDetails) {
                        Column {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = viewModel.workoutDate.value
                            )
                            var showDatePicker by remember { mutableStateOf(false) }

                            Spacer(Modifier.height(16.dp))

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

                            // 1. 🏰 THE HORDE SIEGE DIORAMA
                            if (isSiegeModeEnabled && addedExercises.isNotEmpty()) {
                                val rawMuscle = addedExercises.first().exercise.muscleGroup
                                val domMuscle = viewModel.getBroadMuscleGroup(rawMuscle)
                                val lastWeekVolume = historicalHordeHpMap[domMuscle] ?: 5000

                                // 👇 Look how clean this is now!
                                HordeSiegeBanner(
                                    domMuscle = domMuscle,
                                    lastWeekVolume = lastWeekVolume,
                                    currentSessionVolume = currentSessionVolume,
                                    isArenaExpanded = isArenaExpanded,
                                    hordeRoster = activeHordeLineup,
                                    onToggleArena = { isArenaExpanded = !isArenaExpanded }
                                )

                            } else if (currentSessionVolume > 0) {

                                // 📊 STANDARD SESSION VOLUME CARD (No Siege Mode)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
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

                            // 2. 📜 MISSION BRIEFING
                            if (viewModel.isQuest.value && viewModel.workoutNote.value.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
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
                        }
                    }

                    // ✏️ EDIT TITLE & "EXERCISES" HEADER (Always Visible)
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

            // 📜 CLEAN, STANDARD LAZY COLUMN 📜
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (addedExercises.isEmpty()) {
                    item { Text("No exercises. Add one to start!", modifier = Modifier.padding(top = 16.dp)) }
                } else {
                    items(addedExercises.size) { index ->
                        // 👇 1. Grab the exact monster from the Shared Brain!
                        val isPhoenix = addedExercises[index].exercise.name.contains("Phoenix", ignoreCase = true)
                        val monsterImageId = if (isPhoenix) {
                            com.dravenmiller.swolescroll.R.drawable.monster_phoenix
                        } else {
                            if (activeHordeLineup.isNotEmpty()) activeHordeLineup[index % activeHordeLineup.size]
                            else com.dravenmiller.swolescroll.R.drawable.ic_launcher_foreground
                        }

                        ActiveExerciseCard(
                            workoutExercise = addedExercises[index],
                            index = index,
                            expandedIndex = expandedIndex,
                            isFocusMode = isFocusMode,
                            isSiegeModeEnabled = isSiegeModeEnabled,
                            userWeight = userWeight,
                            prMap = prMapState.value,
                            historyMap = historyMapState.value,
                            monsterHpMap = monsterHpMap,
                            viewModel = viewModel,
                            monsterImageId = monsterImageId, // 👈 2. Pass it down to the Card!
                            onExpandedIndexChange = { expandedIndex = it },
                            onShowDistanceDialog = { exerciseId ->
                                exerciseIdForDistance = exerciseId
                                tempTotalDistance = ""
                                showDistanceDialog = true
                            },
                            onDeleteClick = { exerciseToDelete = addedExercises[index] }
                        )
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
                MiniDungeonControls(
                    addedExercises = addedExercises,
                    expandedIndex = expandedIndex,
                    onExpandedIndexChange = { expandedIndex = it },
                    onPrepareForSuperset = { viewModel.prepareForSuperset(it) },
                    onStartDungeon = { viewModel.startDungeon(it) },
                    onAddToDungeon = { dungeonId ->
                        viewModel.activeDungeonId = dungeonId
                        viewModel.showDialog.value = true
                    },
                    onNewDungeonFresh = { viewModel.startNewDungeon_Fresh() },
                    onAddNewExercise = {
                        viewModel.activeDungeonId = null
                        viewModel.showDialog.value = true
                    }
                )
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
            // 🏃 CARDIO DISTANCE DIALOG
            if (showDistanceDialog) {
                DistanceEntryDialog(
                    tempDistance = tempTotalDistance,
                    onDistanceChange = { tempTotalDistance = it },
                    onCalculateAndSave = {
                        val dist = tempTotalDistance.toDoubleOrNull() ?: 0.0
                        if (dist > 0) {
                            viewModel.applyDistanceToExercise(exerciseIdForDistance, dist)
                        }
                        showDistanceDialog = false
                    },
                    onCancel = { showDistanceDialog = false }
                )
            }

        }
        if (showVictoryOverlay) {
            VictoryOverlay(
                startingXp = capturedStartXp,
                gainedXp = capturedGainedXp,
                lastWeekHp = capturedLastWeekHp, // The math for "Horde Crushed" lives in here!
                improvements = capturedImprovements,
                onContinue = {
                    showVictoryOverlay = false
                    viewModel.saveWorkout(improvements = capturedImprovements, onSaved = onSaveFinished)
                },
                onOpenBattleReport = {
                    showVictoryOverlay = false
                    showBattleReportScreen = true
                },
                workoutName = viewModel.workoutName.value
            )
        }

        if (showBattleReportScreen) {
            com.dravenmiller.swolescroll.features.logworkout.BattleReportScreen(
                improvements = capturedImprovements,
                onBackToRewards = {
                    showBattleReportScreen = false
                    showVictoryOverlay = true
                },
                onExitQuest = {
                    showBattleReportScreen = false
                    viewModel.saveWorkout(improvements = capturedImprovements, onSaved = onSaveFinished)
                }
            )
        }
    }
}
fun calculateSessionImprovements(
    addedExercises: List<com.dravenmiller.swolescroll.model.WorkoutExercise>,
    prMap: Map<String, String>
): List<SkillImprovement> {
    val improvements = mutableListOf<SkillImprovement>()

    addedExercises.forEach { workoutExercise ->
        val type = workoutExercise.exercise.type ?: com.dravenmiller.swolescroll.model.ExerciseType.STRENGTH
        val historyPrString = prMap[workoutExercise.exercise.name]

        val bestSetToday = workoutExercise.sets.maxByOrNull { it.weight }
        val currentW = bestSetToday?.weight ?: 0.0
        val currentR = bestSetToday?.reps ?: 0
        val currentD = bestSetToday?.distance ?: 0.0
        val currentT = bestSetToday?.time ?: 0

        var isNewRecord = false
        var oldLevel = 0f
        var newLevel = 0f
        var bonusDamage = 0
        var generatedPrMessage = "(+Skill Increased)" // Default

        when (type) {
            com.dravenmiller.swolescroll.model.ExerciseType.CARDIO -> {
                val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }
                if (totalDist > 0 && totalSeconds > 0) {
                    val isStairs = workoutExercise.exercise.name.contains("stairs", ignoreCase = true)
                    val currentSpeed = if (isStairs) totalDist / (totalSeconds / 60.0) else totalDist / (totalSeconds / 3600.0)
                    val rawHistory = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                    val historySpeed = if (isStairs && historyPrString?.contains("mph") == true) rawHistory / 60.0 else rawHistory

                    if (currentSpeed > historySpeed) {
                        isNewRecord = true
                        oldLevel = historySpeed.toFloat()
                        newLevel = currentSpeed.toFloat()
                        bonusDamage = ((totalDist) - (rawHistory * (totalSeconds/3600.0))).toInt().coerceAtLeast(0)

                        val diff = currentSpeed - historySpeed
                        generatedPrMessage = if (isStairs) "(+${String.format("%.1f", diff)} stairs/min PR)" else "(+${String.format("%.1f", diff)} mph PR)"
                    }
                }
            }
            com.dravenmiller.swolescroll.model.ExerciseType.LoadedCarry -> {
                val historyW = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                val historyD = historyPrString?.substringAfter("for ")?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0

                if (currentD > 0 && (currentW > historyW || (currentW == historyW && currentD > historyD))) {
                    isNewRecord = true
                    oldLevel = historyW.toFloat()
                    newLevel = currentW.toFloat()
                    if (currentW == historyW && currentD > historyD) newLevel += 0.5f
                    bonusDamage = ((currentW * currentD) - (historyW * historyD)).toInt().coerceAtLeast(0)

                    generatedPrMessage = if (currentW > historyW) "(+${(currentW - historyW).toInt()} lbs PR)" else "(+${(currentD - historyD).toInt()} yds PR)"
                }
            }
            com.dravenmiller.swolescroll.model.ExerciseType.ISOMETRIC -> {
                val maxWeight = workoutExercise.sets.maxOfOrNull { it.weight } ?: 0.0
                val maxTime = workoutExercise.sets.maxOfOrNull { it.time } ?: 0
                val historyWeight = historyPrString?.split(" ")?.firstOrNull()?.toDoubleOrNull() ?: 0.0

                if (maxWeight > historyWeight && maxWeight > 0 && maxTime > 0) {
                    isNewRecord = true
                    oldLevel = historyWeight.toFloat()
                    newLevel = maxWeight.toFloat()
                    bonusDamage = ((maxWeight * maxTime) - (historyWeight * maxTime)).toInt().coerceAtLeast(0)
                    generatedPrMessage = "(+${(maxWeight - historyWeight).toInt()} lbs PR)"
                }
            }
            else -> { // STRENGTH
                val histWeight = historyPrString?.split("x")?.firstOrNull()?.replace("lbs", "")?.trim()?.toDoubleOrNull() ?: 0.0
                val histReps = historyPrString?.split("x")?.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

                val old1RM = if (histWeight > 0) (histWeight * (1.0 + (histReps / 30.0))).toFloat() else 0f
                val new1RM = if (currentW > 0) (currentW * (1.0 + (currentR / 30.0))).toFloat() else 0f

                if (currentR > 0 && (currentW > histWeight || (currentW == histWeight && currentW > 0 && currentR > histReps))) {
                    isNewRecord = true
                    oldLevel = old1RM
                    newLevel = new1RM
                    if (newLevel <= oldLevel) newLevel = oldLevel + 1.5f
                    bonusDamage = ((currentW * currentR) - (histWeight * histReps)).toInt().coerceAtLeast(0)

                    // Logic to see exactly WHAT improved
                    generatedPrMessage = when {
                        currentW > histWeight -> "(+${(currentW - histWeight).toInt()} lbs PR)"
                        currentR > histReps -> "(+${currentR - histReps} Reps PR)"
                        else -> "(+1RM Increased)"
                    }
                }
            }
        }

        if (isNewRecord) {
            improvements.add(
                SkillImprovement(
                    skillName = workoutExercise.exercise.name,
                    old1RM = if (oldLevel > 0f) oldLevel else (newLevel * 0.8f),
                    new1RM = newLevel,
                    bonusDamage = bonusDamage,
                    prMessage = generatedPrMessage // 👈 Pass the message to the UI!
                )
            )
        }
    }
    return improvements
}

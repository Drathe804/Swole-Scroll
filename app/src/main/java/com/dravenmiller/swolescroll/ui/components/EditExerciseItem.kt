package com.dravenmiller.swolescroll.ui.components

import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.util.BodyweightMath
import com.dravenmiller.swolescroll.util.MonsterRoster
import kotlinx.coroutines.delay
import java.text.NumberFormat

@Composable
fun EditExerciseItem(
    workoutExercise: WorkoutExercise,
    exerciseIndex: Int,
    userWeight: Double = 0.0,
    monsterHp: Int = 2000,
    monsterImageId: Int,
    isSiegeModeEnabled: Boolean = true,
    isExpanded: Boolean,
    personalRecord: String?,
    isNewPr: Boolean,
    pastNotes: List<String> = emptyList(),
    onInfoClick: () -> Unit,
    onHeaderClick: () -> Unit,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, Set) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateNote: (String) -> Unit,
    onDelete: () -> Unit,
    onTreadmillSplit: (Int, Double, Int) -> Unit,
    backgroundColor: Color? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExitWarning by remember { mutableStateOf(false) }
    val cardColor = backgroundColor ?: MaterialTheme.colorScheme.surface

    // --- LIVE TIMER STATE ---
    var activeSeconds by remember { mutableIntStateOf(0) }
    val currentSet = workoutExercise.sets.lastOrNull()

    // ... (Keep your Animation Logic for PRs here) ...
    val infiniteTransition = rememberInfiniteTransition(label = "PR_Breath")
    val prColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.secondary,
        targetValue = MaterialTheme.colorScheme.tertiary,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Color"
    )
    val prScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    // 🛡️ CRASH FIX: Force a default type
    val safeType = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

    val isTreadmill = safeType == ExerciseType.TREADMILL
    val isStairs = safeType == ExerciseType.STAIRS

    val primaryVal = currentSet?.weight ?: 0.0
    val secondaryValRaw = if (isTreadmill) (currentSet?.reps ?: 0) else 0
    val isMoving = primaryVal > 0.0

    // ⏱️ TIMER LOGIC
    LaunchedEffect(safeType, isExpanded, isMoving) {
        if (safeType.isCardio && isExpanded && isMoving) {
            val startTime = System.currentTimeMillis()
            val initialSeconds = activeSeconds

            while(true) {
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                activeSeconds = initialSeconds + elapsed
                delay(1000)
            }
        }
    }

    val bwMultiplier = remember(workoutExercise.exercise.name) {
        BodyweightMath.getMultiplier(workoutExercise.exercise.name)
    }

    val currentVolume = remember(workoutExercise.sets, workoutExercise.exercise.isSingleSide) {
        workoutExercise.sets.sumOf { set ->
            val multiplier = if (workoutExercise.exercise.isSingleSide) 2 else 1
            val safeWeight = if (workoutExercise.exercise.isBodyweight) {
                (userWeight * bwMultiplier) + set.weight
            } else {
                set.weight
            }
            val safeDist = set.distance ?: 0.0
            val safeTime = set.time ?: 0

            when (safeType) {
                ExerciseType.STRENGTH -> (safeWeight * set.reps * multiplier).toInt()
                ExerciseType.ISOMETRIC -> (safeWeight * safeTime * multiplier).toInt()
                ExerciseType.LoadedCarry -> (safeWeight * safeDist * multiplier).toInt()
                ExerciseType.TWENTY_ONES -> {
                    val rawVol = (safeWeight * set.reps * multiplier)
                    ((rawVol * 2)/3).toInt()
                }
                else -> 0
            }
        }
    }

    BackHandler(enabled = isExpanded && isMoving && safeType.isCardio) {
        showExitWarning = true
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isExpanded && isMoving && safeType.isCardio) {
                            showExitWarning = true
                        } else {
                            onHeaderClick()
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = workoutExercise.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (isSiegeModeEnabled && !safeType.isCardio) {
                        // ⚔️ DEFEND THE KINGDOM - MONSTER HP BAR ⚔️
                        // 👇 FIX IS HERE: Define isPhoenix, and pass only the needed arguments!
                        val isPhoenix = workoutExercise.exercise.name.contains("Phoenix", ignoreCase = true)

                        val selectedMonsterImage = MonsterRoster.getMonsterFromHorde(
                            muscleGroup = workoutExercise.exercise.muscleGroup,
                            exerciseIndex = exerciseIndex,
                            isPhoenix = isPhoenix
                        )

                        val finalMonsterHp = if (monsterHp > 0) monsterHp else 2000
                        val remainingHp = (finalMonsterHp - currentVolume).coerceAtLeast(0)
                        val hpPercentage = (remainingHp.toFloat() / finalMonsterHp.toFloat()).coerceIn(0f, 1f)
                        val isSlain = remainingHp == 0 && currentVolume > 0
                        val isCrit = isSlain && currentVolume > finalMonsterHp

                        Spacer(Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = monsterImageId),
                                    contentDescription = "Enemy Monster",
                                    modifier = Modifier.size(32.dp).padding(end = 8.dp)
                                )
                                Text(
                                    text = if (isCrit) "💥 CRITICAL HIT!" else if (isSlain) "☠️ MONSTER SLAIN!" else "Monster HP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCrit) Color(0xFFFFB300) else if (isSlain) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                            Text("$remainingHp / $finalMonsterHp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        LinearProgressIndicator(
                            progress = { hpPercentage },
                            modifier = Modifier.fillMaxWidth(0.9f).height(8.dp).padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            strokeCap = StrokeCap.Round
                        )

                        Row(modifier = Modifier.fillMaxWidth(0.9f).padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("⚔️ Damage: ${NumberFormat.getIntegerInstance().format(currentVolume)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            if (safeType == ExerciseType.ISOMETRIC) {
                                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }
                                if (totalSeconds > 0) Text("TUT: ${String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    } else {
                        // 📊 STANDARD UI MODE 📊
                        if (safeType == ExerciseType.ISOMETRIC) {
                            val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }
                            if (totalSeconds > 0) {
                                Text("TUT: ${String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        } else if (currentVolume > 0) {
                            Text("Vol: ${NumberFormat.getIntegerInstance().format(currentVolume)} lbs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 🏆 PR LOGIC
                    if (personalRecord != null) {
                        if(isNewPr) {
                            Text(
                                text = "NEW PR: $personalRecord",
                                style = MaterialTheme.typography.labelMedium,
                                color = prColor,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.graphicsLayer { scaleX = prScale; scaleY = prScale; transformOrigin = TransformOrigin(0f, 0.5f) }
                            )
                        } else {
                            val prText = remember(personalRecord, safeType) {
                                if (personalRecord.contains("mi") ||
                                    personalRecord.contains("stairs") ||
                                    personalRecord.contains("yds") ||
                                    personalRecord.contains("mph") ||
                                    personalRecord.contains("/min")
                                ) {
                                    personalRecord
                                } else if (safeType.isCardio && personalRecord.contains("lbs")) {
                                    val rawNum = personalRecord.split(" ").firstOrNull() ?: personalRecord
                                    "Max Lvl $rawNum"
                                } else {
                                    if (safeType == ExerciseType.ISOMETRIC) {
                                        val rawNum = personalRecord.split(" ").firstOrNull() ?: personalRecord
                                        "$rawNum lbs"
                                    } else {
                                        if (personalRecord.contains("lbs")) personalRecord else "$personalRecord lbs"
                                    }
                                }
                            }
                            Text(text = "PR: $prText", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Column {
                    Row {
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                        IconButton(onClick = onInfoClick) { Icon(Icons.Default.Info, "History", tint = MaterialTheme.colorScheme.primary) }
                    }
                    Icon(modifier = Modifier.size(32.dp).align(Alignment.CenterHorizontally), imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sets") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Notes") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedTab == 0) {
                        Column(
                            modifier = Modifier
                        ) {
                            if (safeType.isCardio && isExpanded) {

                                Text("Live Controls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                                Text(
                                    text = String.format("%02d:%02d", activeSeconds / 60, activeSeconds % 60),
                                    style = MaterialTheme.typography.displayMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                                    color = if (isMoving) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )

                                CardioControls(
                                    isTreadmill = isTreadmill,
                                    primaryValue = primaryVal,
                                    secondaryValueRaw = secondaryValRaw,
                                    onPrimaryChange = { newValue -> onTreadmillSplit(activeSeconds, newValue, secondaryValRaw) },
                                    onSecondaryChange = { newRaw -> onTreadmillSplit(activeSeconds, primaryVal, newRaw) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            // Headers
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                Text("Set", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall)

                                when {
                                    isTreadmill -> {
                                        Text("Dist", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Time", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Spd", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Inc", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall)
                                    }
                                    safeType.isCardio -> {
                                        Text(if (isStairs) "Stairs" else "Dist", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Time", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Lvl", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall)
                                    }
                                    safeType == ExerciseType.ISOMETRIC -> {
                                        Text("Lbs", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Time", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                    }
                                    safeType == ExerciseType.LoadedCarry -> {
                                        Text("Lbs", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Dist", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                    }
                                    else -> {
                                        Text("Lbs", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reps", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(modifier = Modifier.width(30.dp))
                            }

                            workoutExercise.sets.forEachIndexed { index, set ->
                                Column {
                                    SetInputRow(
                                        setNumber = index + 1,
                                        set = set,
                                        type = safeType,
                                        isEditable = index == workoutExercise.sets.lastIndex,
                                        isStairs = isStairs,
                                        isTreadmill = isTreadmill,
                                        onUpdate = { updatedSet -> onUpdateSet(index, updatedSet) },
                                        onRemove = { onRemoveSet(index) }
                                    )

                                    if (workoutExercise.exercise.isBodyweight && userWeight > 0) {
                                        val totalEffectiveLoad = (userWeight * bwMultiplier) + set.weight
                                        Text(
                                            text = "Total Load: ${String.format("%.1f", totalEffectiveLoad)} lbs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontStyle = FontStyle.Italic,
                                            modifier = Modifier.padding(start = 36.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = onAddSet) { Text("+ Add Set") }
                        }
                    } else {
                        // Notes tab
                        OutlinedTextField(value = workoutExercise.note ?: "", onValueChange = onUpdateNote, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().height(150.dp), maxLines = 5)
                        if (pastNotes.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Past Notes:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            pastNotes.takeLast(3).reversed().forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            if (!isExpanded && workoutExercise.sets.isNotEmpty()) {
                Text("${workoutExercise.sets.size} Sets", style = MaterialTheme.typography.bodySmall)
            }
            if (showExitWarning) {
                AlertDialog(
                    onDismissRequest = { showExitWarning = false },
                    title = { Text("Workout in progress") },
                    text = { Text("Your timer is running. Are you sure you want to leave?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitWarning = false
                                onHeaderClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Pause & Close")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitWarning = false }) {
                            Text("Keep Running")
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 🛠️ HELPER FUNCTIONS
// -------------------------------------------------------------------------

@Composable
fun SetInputRow(
    setNumber: Int,
    set: Set,
    type: ExerciseType,
    isEditable: Boolean,
    isStairs: Boolean,
    isTreadmill: Boolean,
    onUpdate: (Set) -> Unit,
    onRemove: () -> Unit
) {
    val safeTime = set.time ?: 0
    var weightText by remember(set.id) { mutableStateOf(if (set.weight == 0.0) "" else set.weight.toString().removeSuffix(".0")) }

    var repsText by remember(set.id) {
        mutableStateOf(
            if (isTreadmill) {
                if (set.reps == 0) "" else (set.reps / 10.0).toString()
            } else {
                if (set.reps == 0) "" else set.reps.toString()
            }
        )
    }

    var distanceText by remember(set.id) { mutableStateOf(if (set.distance == 0.0) "" else set.distance.toString().removeSuffix(".0")) }
    var minutesText by remember(set.id) { mutableStateOf(if (safeTime == 0) "" else (safeTime / 60).toString()) }
    var secondsText by remember(set.id) { mutableStateOf(if (safeTime == 0) "" else (safeTime % 60).toString()) }

    val readOnlyTextStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$setNumber", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (type.isCardio) {
            if (isEditable) {
                Surface(
                    modifier = Modifier.weight(1f).height(56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        CompactTextField(
                            value = distanceText,
                            onValueChange = { if (validateDecimal(it)) { distanceText = it; onUpdate(set.copy(distance = it.toDoubleOrNull() ?: 0.0)) } },
                            placeholder = if (isStairs) "Stairs" else "Mi",
                            modifier = Modifier.weight(0.8f),
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                        CompactTimeInput(minutesText, secondsText, modifier = Modifier.weight(1f),
                            onMinChange = { minutesText = it; updateTime(it, secondsText) { t -> onUpdate(set.copy(time = t)) } },
                            onSecChange = { secondsText = it; updateTime(minutesText, it) { t -> onUpdate(set.copy(time = t)) } }
                        )

                        if (isTreadmill) {
                            CompactTextField(
                                value = weightText,
                                onValueChange = { if (validateDecimal(it)) { weightText = it; onUpdate(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) } },
                                placeholder = "Spd",
                                modifier = Modifier.weight(0.6f),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                            IconButton(
                                onClick = {
                                    val valInWeightSlot = set.weight
                                    val valInRepsSlot = set.reps / 10.0
                                    onUpdate(set.copy(weight = valInRepsSlot, reps = (valInWeightSlot * 10).toInt()))
                                },
                                modifier = Modifier.size(32.dp).padding(horizontal = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, "Swap", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            }
                            CompactTextField(
                                value = repsText,
                                onValueChange = { if (validateDecimal(it)) { repsText = it; val decimalValue = it.toDoubleOrNull() ?: 0.0; onUpdate(set.copy(reps = (decimalValue * 10).toInt())) } },
                                placeholder = "Inc",
                                modifier = Modifier.weight(0.6f),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        } else {
                            CompactTextField(
                                value = weightText,
                                onValueChange = { if (validateDecimal(it)) { weightText = it; onUpdate(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) } },
                                placeholder = "Lvl",
                                modifier = Modifier.weight(0.8f),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        }
                    }
                }
            } else {
                Text("${set.distance} ${if (isStairs) "stairs" else "mi"}", modifier = Modifier.weight(0.8f), style = readOnlyTextStyle)
                Text(set.timeFormatted(), modifier = Modifier.weight(1f), style = readOnlyTextStyle, textAlign = TextAlign.Center)

                if (isTreadmill) {
                    Text("Spd ${set.weight}", modifier = Modifier.weight(0.6f), style = readOnlyTextStyle, textAlign = TextAlign.Center)
                    Text("${set.reps / 10.0}%", modifier = Modifier.weight(0.6f), style = readOnlyTextStyle, textAlign = TextAlign.End)
                } else {
                    Text("Lvl ${set.weight}".removeSuffix(".0"), modifier = Modifier.weight(0.8f), style = readOnlyTextStyle, textAlign = TextAlign.End)
                }
            }
        } else if (type == ExerciseType.ISOMETRIC) {
            if (isEditable) {
                OutlinedTextField(value = weightText, onValueChange = { if (validateDecimal(it)) { weightText = it; onUpdate(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) } }, modifier = Modifier.weight(1f), placeholder = { Text("Lbs") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
                Spacer(Modifier.width(8.dp))
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = minutesText, onValueChange = { if (it.all { c -> c.isDigit() }) {minutesText = it; updateTime(it, secondsText){s->onUpdate(set.copy(time=s))}} }, modifier = Modifier.weight(1f), placeholder = { Text("Min") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                    Text(":", modifier = Modifier.padding(horizontal = 2.dp))
                    OutlinedTextField(value = secondsText, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) {secondsText = it; updateTime(minutesText, it){s->onUpdate(set.copy(time=s))}} }, modifier = Modifier.weight(1f), placeholder = { Text("Sec") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                }
            } else {
                Text("${set.weight} lbs", modifier = Modifier.weight(1f), style = readOnlyTextStyle)
                Text(set.timeFormatted(), modifier = Modifier.weight(1f), style = readOnlyTextStyle)
            }
        } else if (type == ExerciseType.LoadedCarry) {
            if (isEditable) {
                OutlinedTextField(value = weightText, onValueChange = { if (validateDecimal(it)) { weightText = it; onUpdate(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) } }, modifier = Modifier.weight(1f), placeholder = { Text("Lbs") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = distanceText, onValueChange = { if (validateDecimal(it)) { distanceText = it; onUpdate(set.copy(distance = it.toDoubleOrNull() ?: 0.0)) } }, modifier = Modifier.weight(1f), placeholder = { Text("Yds") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
            } else {
                Text("${set.weight} lbs", modifier = Modifier.weight(1f), style = readOnlyTextStyle)
                Text("${set.distance} yds", modifier = Modifier.weight(1f), style = readOnlyTextStyle)
            }
        } else {
            // STRENGTH
            if (isEditable) {
                OutlinedTextField(value = weightText, onValueChange = { if (validateDecimal(it)) { weightText = it; onUpdate(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) } }, modifier = Modifier.weight(1f), placeholder = { Text("Lbs") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = repsText, onValueChange = { if (it.all { c -> c.isDigit() }) { repsText = it; onUpdate(set.copy(reps = it.toIntOrNull() ?: 0)) } }, modifier = Modifier.weight(1f), placeholder = { Text("Reps") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
            } else {
                Text("${set.weight} lbs", modifier = Modifier.weight(1f), style = readOnlyTextStyle)
                Text("${set.reps} reps", modifier = Modifier.weight(1f), style = readOnlyTextStyle)
            }
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun CardioControls(
    isTreadmill: Boolean,
    primaryValue: Double,
    secondaryValueRaw: Int,
    onPrimaryChange: (Double) -> Unit,
    onSecondaryChange: (Int) -> Unit
) {
    val primaryLabel = if(isTreadmill) "Speed" else "Level"
    val primaryStep = 0.5

    val displaySecondary = if (secondaryValueRaw == 0) "0.0" else (secondaryValueRaw / 10.0).toString()

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ControlCluster(
            label = primaryLabel,
            value = "$primaryValue",
            onDecrease = { onPrimaryChange((primaryValue - primaryStep).coerceAtLeast(0.0)) },
            onIncrease = { onPrimaryChange(primaryValue + primaryStep) }
        )

        if (isTreadmill) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp).width(200.dp))
            ControlCluster(
                label = "Incline",
                value = displaySecondary,
                onDecrease = { onSecondaryChange((secondaryValueRaw - 5).coerceAtLeast(0)) },
                onIncrease = { onSecondaryChange(secondaryValueRaw + 5) }
            )
        }
    }
}

@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType,
    imeAction: ImeAction
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.padding(horizontal = 4.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun CompactTimeInput(
    minutes: String,
    seconds: String,
    modifier: Modifier = Modifier,
    onMinChange: (String) -> Unit,
    onSecChange: (String) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CompactTextField(
            value = minutes,
            onValueChange = { if (it.all { c -> c.isDigit() }) onMinChange(it) },
            placeholder = "M",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )
        Text(":", style = MaterialTheme.typography.bodyLarge)
        CompactTextField(
            value = seconds,
            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) onSecChange(it) },
            placeholder = "S",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
    }
}

@Composable
fun ControlCluster(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = onDecrease, modifier = Modifier.size(48.dp)) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FilledIconButton(onClick = onIncrease, modifier = Modifier.size(48.dp)) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

// Logic Helpers
fun validateDecimal(text: String): Boolean {
    return text.count { it == '.' } <= 1 && text.all { it.isDigit() || it == '.' }
}

fun updateTime(minStr: String, secStr: String, onUpdate: (Int) -> Unit) {
    val min = minStr.toIntOrNull() ?: 0
    val sec = secStr.toIntOrNull() ?: 0
    onUpdate((min * 60) + sec)
}

package com.dravenmiller.swolescroll.features.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.ui.components.GraphMode
import com.dravenmiller.swolescroll.ui.components.OneRepMaxGraph // 👈 Don't forget this import!
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    exerciseName: String,
    viewModel: ExerciseHistoryViewModel,
    onBackClick: () -> Unit
) {
    val history by viewModel.history.collectAsState()

    // 1. OBSERVE GRAPH DATA 📉
    // (Make sure your ExerciseHistoryViewModel has this variable!)
    val graphPoints by viewModel.graphData.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var graphMode by remember { mutableStateOf(GraphMode.SMART) } // State for the toggle

    // 2. TRIGGER CALCULATION 🧮
    LaunchedEffect(exerciseName) {
        viewModel.generateOneRepMaxHistory(exerciseName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("History") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Notes") })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    // --- TAB: HISTORY ---

                    // 3. THE GRAPH CARD (Only show if we have data) 📉
                    if (graphPoints.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Estimated 1RM Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // THE TOGGLE CHIPS
                                    ScrollableTabRow(
                                        selectedTabIndex = graphMode.ordinal,
                                        edgePadding = 0.dp,
                                        containerColor = Color.Transparent,
                                        indicator = { /* Optional: Hide indicator if using chips */ },
                                        divider = {}
                                    ) {
                                        GraphMode.values().forEach { mode ->
                                            val isSelected = graphMode == mode
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { graphMode = mode },
                                                label = { Text(mode.name) },
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    // THE GRAPH
                                    OneRepMaxGraph(
                                        data = graphPoints,
                                        selectedMode = graphMode,
                                        modifier = Modifier.fillMaxWidth().height(200.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. THE LOG LIST (Below the graph)
                    if (history.isEmpty()) {
                        item { Text("No history found.") }
                    } else {
                        items(history) { entry ->
                            HistoryCard(entry, exerciseName)
                        }
                    }

                } else {
                    // --- TAB: NOTES ---
                    val notesOnly = history.filter { it.note.isNotBlank() }
                    if (notesOnly.isEmpty()) {
                        item {
                            Text(
                                "No notes recorded for this exercise yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(notesOnly) { entry -> NoteCard(entry) }
                    }
                }
            }
        }
    }
}

// ... (HistoryCard and NoteCard functions remain exactly the same as you had them)
@Composable
fun HistoryCard(
    entry: HistoryEntry,
    exerciseName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(entry.date))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 🧠 INTELLIGENT EXERCISE TYPE DETECTION
            val isCarry = exerciseName.contains("Carry", true) ||
                    exerciseName.contains("Farmer", true) ||
                    exerciseName.contains("Yoke", true)

            // It's "Cardio" only if it has distance/time AND IS NOT A CARRY
            val hasDistance = entry.sets.any { (it.distance ?: 0.0) > 0.0 }
            val isCardioLikely = !isCarry && hasDistance && entry.sets.any { it.time > 0 }

            // --- HEADER SUMMARY ---
            if (isCarry) {
                // 🚛 CARRY SUMMARY (Max Weight & Total Distance)
                val bestSet = entry.sets.maxByOrNull { it.weight }
                val maxWeight = bestSet?.weight ?: 0.0
                val totalDist = entry.sets.sumOf { it.distance ?: 0.0 }

                Text(
                    text = "Best: $maxWeight lbs | Vol: $totalDist yds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

            } else if (isCardioLikely) {
                // 🏃‍♂️ CARDIO SUMMARY (Total Distance & Time)
                val totalDist = entry.sets.sumOf { it.distance ?: 0.0 }
                val totalSeconds = entry.sets.sumOf { it.time ?: 0 }

                val m = totalSeconds / 60
                val s = totalSeconds % 60
                val timeStr = String.format("%d:%02d", m, s)
                val distStr = String.format("%.2f", totalDist).trimEnd('0').trimEnd('.')

                val dominantSetGroup = entry.sets
                    .groupBy { Pair(it.reps, it.weight) }
                    .entries
                    .maxWithOrNull(
                        compareBy<Map.Entry<Pair<Int, Double>, List<com.dravenmiller.swolescroll.model.Set>>> {
                            it.value.sumOf { s -> s.time }
                        }.thenBy {
                            it.value.sumOf { s -> s.distance ?: 0.0 }
                        }
                    )

                val domReps = dominantSetGroup?.key?.first ?: 0
                val domWeight = dominantSetGroup?.key?.second ?: 0.0

                val details = mutableListOf<String>()
                val isTreadmill = exerciseName.contains("Treadmill", true)

                if (isTreadmill) {
                    if (domWeight > 0) details.add("Lvl $domWeight")
                    if (domReps > 0) details.add("${domReps/10.0}% Inc")
                } else {
                    if (domWeight > 0) details.add("Lvl $domWeight")
                }

                val detailStr = if (details.isNotEmpty()) " (@ ${details.joinToString(", ")})" else ""
                val unit = if (exerciseName.contains("Stair", true)) "stairs" else "mi"

                Text(
                    text = "Total: $distStr $unit in $timeStr$detailStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- SET LIST ---
            val visibleSets = if (isCardioLikely) {
                entry.sets.filter { (it.distance ?: 0.0) > 0.0 }
            } else {
                entry.sets
            }

            visibleSets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Set ${index + 1}", style = MaterialTheme.typography.bodyMedium)

                    val formattedText = when {
                        // 1. CARRY (Explicit Check First!)
                        isCarry -> {
                            val dist = set.distance ?: 0.0
                            "${set.weight} lbs for $dist yds"
                        }

                        // 2. CARDIO
                        isCardioLikely && (set.distance ?: 0.0) > 0 -> {
                            val d = set.distance ?: 0.0
                            val t = set.timeFormatted()

                            val isTreadmillRow = exerciseName.contains("Treadmill", true)
                            val speed = set.reps / 10.0
                            val rowWeight = set.weight

                            val extras = mutableListOf<String>()
                            if (isTreadmillRow) {
                                if (rowWeight > 0) extras.add("Lvl $rowWeight")
                                if (speed > 0) extras.add("$speed% Inc")
                            } else {
                                if (rowWeight > 0) extras.add("Lvl $rowWeight")
                            }

                            val extraStr = if (extras.isNotEmpty()) " (${extras.joinToString(", ")})" else ""
                            val rowUnit = if (exerciseName.contains("Stair", true)) "stairs" else "mi"
                            "$d $rowUnit in $t$extraStr"
                        }

                        // 3. ISOMETRIC (Time based, no reps)
                        set.time > 0 && set.reps == 0 -> {
                            val w = if(set.weight > 0) "${set.weight} lbs for " else ""
                            "$w${set.timeFormatted()}"
                        }

                        // 4. STRENGTH (Standard)
                        else -> "${set.weight} lbs x ${set.reps} reps"
                    }

                    Text(text = formattedText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun NoteCard(entry: HistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(entry.date))
            Text(text = dateString, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = entry.note, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

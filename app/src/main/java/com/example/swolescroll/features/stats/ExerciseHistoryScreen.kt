package com.example.swolescroll.features.stats

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // STATE: Track which tab is open (0 = History, 1 = Notes)
    var selectedTab by remember { mutableStateOf(0) }

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

            // 1. THE TAB ROW (The part you were missing!)
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("History") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Notes") }
                )
            }

            // 2. THE CONTENT LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    // --- HISTORY TAB ---
                    if (history.isEmpty()) {
                        item { Text("No history found.") }
                    } else {
                        items(history) { entry ->
                            HistoryCard(entry)
                        }
                    }
                } else {
                    // --- NOTES TAB ---
                    // Only show entries that actually have notes
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
                        items(notesOnly) { entry ->
                            NoteCard(entry)
                        }
                    }
                }
            }
        }
    }
}

// 3. CARD FOR HISTORY (Weights)
@Composable
fun HistoryCard(entry: HistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(entry.date))
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            entry.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set ${index + 1}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val setCheck = set
                    val isCardio = (setCheck.distance ?: 0.0) > 0 && setCheck.reps == 0 && setCheck.weight == 0.0
                    val isCarry = (setCheck.distance ?: 0.0) > 0 && setCheck.weight > 0
                    val isIso = setCheck.time > 0 && setCheck.weight > 0 && (setCheck.distance ?: 0.0) == 0.0

                    val formattedText = when {
                        isCardio -> "${setCheck.distance ?: 0.0} mi in ${setCheck.timeFormatted()}"
                        isCarry -> "${setCheck.weight} lbs for ${setCheck.distance ?: 0.0} yds"
                        isIso -> "${setCheck.weight} lbs for ${setCheck.timeFormatted()}"
                        else -> "${setCheck.weight} lbs x ${setCheck.reps} reps"
                    }
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// 4. CARD FOR NOTES (Advice)
@Composable
fun NoteCard(entry: HistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Different color so it feels distinct
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(entry.date))

            Text(
                text = dateString,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.note,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

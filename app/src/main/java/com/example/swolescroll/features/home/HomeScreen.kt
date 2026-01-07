package com.example.swolescroll.features.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swolescroll.data.BackupManager
import com.example.swolescroll.data.MockData
import com.example.swolescroll.model.Workout
import com.example.swolescroll.ui.components.WorkoutCard

// 1. THE CONTROLLER (Handles Logic & ViewModel)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onWorkoutClick: (Workout) -> Unit,
    onFabClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    // Collect Data from ViewModel
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Setup File Picker (Launcher)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    // Call the UI, passing down the Data and Actions (but NOT the ViewModel)
    HomeScreenContent(
        workouts = workouts,
        onWorkoutClick = onWorkoutClick,
        onFabClick = onFabClick,
        onStatsClick = onStatsClick,
        onShareClick = {
            val uri = BackupManager.getBackupUri(context)
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
            }
        },
        onBackupClick = {viewModel.backupNow()},
        onImportClick = {
            importLauncher.launch("application/json")
        }
    )
}

// 2. THE UI (Stateless - Pure Design)
// We moved the Scaffold here so the Preview can use it without crashing!
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    workouts: List<Workout>,
    onWorkoutClick: (Workout) -> Unit,
    onFabClick: () -> Unit,
    onStatsClick: () -> Unit,
    onShareClick: () -> Unit,
    onImportClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    var showSettingsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // The Watermark Title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "The Swole Scroll",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Est. 2025 • Draven Miller",
                            style = MaterialTheme.typography.labelMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                // LEFT: Settings Gear
                navigationIcon = {
                    Column {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }

                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Backup to downloads") },
                                onClick = {
                                    showSettingsMenu = false
                                    onBackupClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Backup File") },
                                onClick = {
                                    showSettingsMenu = false
                                    onShareClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Backup File") },
                                onClick = {
                                    showSettingsMenu = false
                                    onImportClick()
                                }
                            )
                        }
                    }
                },
                // RIGHT: Stats Star
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.Star, contentDescription = "Stats")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(innerPadding)
        ) {
            if (workouts.isEmpty()) {
                item {
                    Text(
                        text = buildAnnotatedString {
                            append("No scrolls found. Start lifting now! Begin your ")
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append("Legend")
                            }
                            append("!")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(workouts) { workout ->
                    WorkoutCard(
                        workout = workout,
                        modifier = Modifier.clickable { onWorkoutClick(workout) }
                    )
                }
            }
        }
    }
}

// 3. THE PREVIEW
// Notice: We preview 'HomeScreenContent' (which takes data), not 'HomeScreen' (which takes ViewModel)
@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreenContent(
        workouts = MockData.sampleWorkouts,
        onWorkoutClick = {},
        onFabClick = {},
        onStatsClick = {},
        onShareClick = {},
        onBackupClick = {},
        onImportClick = {}
    )
}

package com.dravenmiller.swolescroll.features.home

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome // ⚔️ RPG Icon
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dravenmiller.swolescroll.data.BackupManager
import com.dravenmiller.swolescroll.data.MockData
import com.dravenmiller.swolescroll.features.profile.UserProfileViewModel
import com.dravenmiller.swolescroll.features.quests.QuestBoardDialog // 👈 Import
import com.dravenmiller.swolescroll.model.Workout
import com.dravenmiller.swolescroll.ui.components.WorkoutCard

// 1. THE CONTROLLER (Handles Logic & ViewModel)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onWorkoutClick: (Workout) -> Unit,
    onFabClick: () -> Unit,
    onStatsClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    userViewModel: UserProfileViewModel = viewModel(),
) {
    // Collect Data from ViewModel
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val showMonthlyPrompt by userViewModel.showMonthlyPrompt.collectAsState()
    var weighInValue by remember { mutableStateOf("") }

    // 👇 1. QUEST LOGIC: Show Dialog if needed
    if (viewModel.showQuestDialog.value) {
        QuestBoardDialog(
            onDismiss = { viewModel.showQuestDialog.value = false },
            onAcceptQuest = { difficulty -> viewModel.acceptQuest(difficulty) }
        )
    }

    // 👇 2. QUEST LOGIC: Handle Navigation
    if (viewModel.navigateToLog.value) {
        viewModel.onNavigationHandled()
        onFabClick() // Re-use the "Go to Log" callback
    }

    // Setup File Picker (Launcher)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri)
        }
    }

    if (showMonthlyPrompt) {
        AlertDialog(
            onDismissRequest = { userViewModel.dismissPrompt() },
            title = { Text("Monthly Weigh-In") },
            text = {
                Column {
                    Text("It's been 30 days! Update your weight to keep difficulty calculations accurate.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weighInValue,
                        onValueChange = { weighInValue = it },
                        label = { Text("Current Weight") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val w = weighInValue.toDoubleOrNull()
                    if (w != null && w > 0) {
                        // We fetch current profile to keep name/difficulty same
                        // Ideally ViewModel handles this cleaner, but this works for now
                        val current = userViewModel.userProfile.value
                        if (current != null) {
                            userViewModel.saveProfile(current.name, w, current.defaultDifficulty)
                        }
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { userViewModel.dismissPrompt() }) { Text("Skip") }
            }
        )
    }

    // Call the UI
    HomeScreenContent(
        workouts = workouts,
        onWorkoutClick = onWorkoutClick,
        onFabClick = onFabClick,
        // 👇 PASS THE CLICK ACTION DOWN
        onQuestClick = { viewModel.showQuestDialog.value = true },
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
        },
        onNavigateToProfile = onNavigateToProfile
    )
}

// 2. THE UI (Stateless - Pure Design)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    workouts: List<Workout>,
    onWorkoutClick: (Workout) -> Unit,
    onFabClick: () -> Unit,
    onQuestClick: () -> Unit, // 👈 New Parameter
    onStatsClick: () -> Unit,
    onShareClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onImportClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    var showSettingsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
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
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
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
                actions = {
                    // 👤 PROFILE BUTTON
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person, // Or Icons.Default.AccountCircle
                            contentDescription = "Profile"
                        )
                    }
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.Star, contentDescription = "Stats")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Space between buttons
            ) {
                // ⚔️ QUEST BUTTON
                FloatingActionButton(
                    onClick = onQuestClick,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.AutoMode, contentDescription = "Quest")
                }

                // ➕ ADD BUTTON
                FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Workout")
                }
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
@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreenContent(
        workouts = MockData.sampleWorkouts,
        onWorkoutClick = {},
        onFabClick = {},
        onQuestClick = {}, // 👈 Dummy callback for preview
        onStatsClick = {},
        onShareClick = {},
        onBackupClick = {},
        onImportClick = {},
        onNavigateToProfile = {}
    )
}

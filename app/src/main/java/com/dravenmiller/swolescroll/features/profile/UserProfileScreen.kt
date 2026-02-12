package com.dravenmiller.swolescroll.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dravenmiller.swolescroll.features.quests.QuestDifficulty
import com.dravenmiller.swolescroll.ui.components.SwoleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBackClick: () -> Unit,
    viewModel: UserProfileViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()

    // Local State for Editing
    var name by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("RAID") }

    // Load initial values when profile loads
    LaunchedEffect(userProfile) {
        userProfile?.let {
            name = it.name
            weightStr = if (it.bodyWeight > 0) it.bodyWeight.toString() else ""
            difficulty = it.defaultDifficulty
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adventurer Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 👤 AVATAR / ICON
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            // 📝 NAME INPUT
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hero Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(Modifier.height(16.dp))

            // ⚖️ WEIGHT INPUT
            OutlinedTextField(
                value = weightStr,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) weightStr = it },
                label = { Text("Body Weight (lbs)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Used for Calisthenics calculations") }
            )

            Spacer(Modifier.height(24.dp))

            // ⚔️ DIFFICULTY SELECTOR
            Text("Session Duration (Difficulty)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Save these as "EASY", "NORMAL", "HARD"
                DifficultyChip("Easy", difficulty) { difficulty = "EASY" }
                DifficultyChip("Normal", difficulty) { difficulty = "NORMAL" }
                DifficultyChip("Hard", difficulty) { difficulty = "HARD" }
            }

            // Explanation Text Updates
            val difficultyDesc = when(difficulty) {
                "EASY" -> "Short on time. ~15-25 mins. Fewer exercises."
                "NORMAL" -> "The standard workout. ~45 mins. Good balance."
                "HARD" -> "Marathon session. 60-75+ mins. High volume."
                else -> "Select a duration preference."
            }
            Text(
                text = difficultyDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            // 💾 SAVE BUTTON
            SwoleButton(
                text = "Save Profile",
                onClick = {
                    val w = weightStr.toDoubleOrNull() ?: 0.0
                    viewModel.saveProfile(name, w, difficulty)
                    onBackClick()
                }
            )
        }
    }
}

@Composable
fun DifficultyChip(
    label: String,
    selected: String,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = (label == selected),
        onClick = onSelect,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

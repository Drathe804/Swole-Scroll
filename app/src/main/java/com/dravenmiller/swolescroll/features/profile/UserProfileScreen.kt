package com.dravenmiller.swolescroll.features.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
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
    val isSiegeModeEnabled by viewModel.isSiegeModeEnabled.collectAsState()
    val lifetimeVolume by viewModel.lifetimeVolume.collectAsState()



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

            // 🛡️ RPG LEVEL & XP BAR (THE DESTINY BAR)
            val lifetimeVolume by viewModel.lifetimeVolume.collectAsState()
            val draftVolume by viewModel.draftVolume.collectAsState() // 🔮 Potential Volume

            // 1. Math for Current Level
            val currentLevel = com.dravenmiller.swolescroll.util.RpgMath.calculateLevel(lifetimeVolume)
            val currentLevelBaseXp = com.dravenmiller.swolescroll.util.RpgMath.xpRequiredForLevel(currentLevel)
            val nextLevelXp = com.dravenmiller.swolescroll.util.RpgMath.xpRequiredForLevel(currentLevel + 1)

            // 2. Math for Potential Level (If you finished the draft)
            val totalPotentialVolume = lifetimeVolume + draftVolume
            val potentialLevel = com.dravenmiller.swolescroll.util.RpgMath.calculateLevel(totalPotentialVolume)

            // 3. Progress Calculations (Math is fun!)
            val totalXpNeededForNextLevel = nextLevelXp - currentLevelBaseXp

            val xpEarnedInCurrentLevel = lifetimeVolume - currentLevelBaseXp
            val xpPotentialInCurrentLevel = (totalPotentialVolume - currentLevelBaseXp).coerceAtLeast(0)

            val earnedPercentage = (xpEarnedInCurrentLevel.toFloat() / totalXpNeededForNextLevel.toFloat()).coerceIn(0f, 1f)
            val totalPotentialPercentage = (xpPotentialInCurrentLevel.toFloat() / totalXpNeededForNextLevel.toFloat()).coerceIn(0f, 1f)

            // 4. Draw the Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(0.9f).padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    // Level Badge
                    Text(
                        text = "Level $currentLevel Adventurer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    if (potentialLevel > currentLevel) {
                        Text(
                            text = "Potential Lvl ${potentialLevel}!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981) // Green
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🌈 THE CUSTOM DESTINY XP BAR 🌈
                    DestinyXpBar(
                        earnedPercentage = earnedPercentage,
                        totalPercentage = totalPotentialPercentage,
                        modifier = Modifier.fillMaxWidth().height(16.dp) // Make it nice and thick
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // The Numbers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // Current Locked-in XP / Next Goal XP
                        Text(
                            text = "XP: ${java.text.NumberFormat.getIntegerInstance().format(lifetimeVolume)} / ${java.text.NumberFormat.getIntegerInstance().format(nextLevelXp)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Show unclaimed volume if it exists
                        if (draftVolume > 0) {
                            Text(
                                text = "🔮 Unclaimed: ${java.text.NumberFormat.getIntegerInstance().format(draftVolume)} lbs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Next: Lvl ${currentLevel + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

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
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // 🐉 SIEGE MODE TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "RPG Siege Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Turn your workouts into epic boss fights against your past volume records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = isSiegeModeEnabled,
                    onCheckedChange = { viewModel.toggleSiegeMode(it) }
                )
            }

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

// 🌈 THE DESTINY XP BAR 🌈 (Now a true seamless, flowing rainbow!)
@Composable
fun DestinyXpBar(
    earnedPercentage: Float,
    totalPercentage: Float, // Earned + Unclaimed
    modifier: Modifier = Modifier
) {
    // 🎨 THE SEAMLESS RAINBOW
    // Includes your extra colors at the end to guarantee a flawless TileMode loop!
    val seamlessRainbow = remember {
        listOf(
            Color(0xFF9C27B0), // Purple (Start)
            Color(0xFF3F51B5), // Deep Blue
            Color(0xFF2196F3), // Bright Blue
            Color(0xFF00BCD4), // Cyan
            Color(0xFF4CAF50), // Green
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFF9800), // Orange
            Color(0xFFF44336), // Red
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0), // Purple
            //Color(0xFF3F51B5), // Deep Blue (Loop runway)
            //Color(0xFF2196F3)  // Bright Blue (Loop runway)
        )
    }

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    // 🌊 HERE IS THE MISSING VARIABLE! (The Animation Driver) 🌊
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow_flow")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart // Resets instantly at 1f
        ),
        label = "flow_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cornerRadius = height / 2f

        // 🎛️ Your custom gradient width modifier
        val gradientWidthMultiplier = 1.2f
        val gradientWidth = width * gradientWidthMultiplier

        // The animation moves exactly one 'gradientWidth' over its lifecycle
        val offset = flowPhase * gradientWidth

        // 🌊 THE MATH-PERFECT BRUSH 🌊
        val flowingRainbowBrush = Brush.horizontalGradient(
            colors = seamlessRainbow,
            startX = offset,
            endX = offset + gradientWidth, // Matches the travel distance exactly!
            tileMode = TileMode.Repeated
        )

        // 1. Draw the Empty Track
        drawRoundRect(
            color = trackColor,
            size = size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // 2. Draw Unclaimed Draft XP (Holographic / Transparent)
        val totalPotentialWidth = width * totalPercentage.coerceIn(0f, 1f)
        if (totalPotentialWidth > 0f) {
            drawRoundRect(
                brush = flowingRainbowBrush,
                size = Size(totalPotentialWidth, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                alpha = 0.4f // Ghostly potential XP!
            )
        }

        // 3. Draw Earned XP (Solid, locked-in)
        val earnedWidth = width * earnedPercentage.coerceIn(0f, 1f)
        if (earnedWidth > 0f) {
            drawRoundRect(
                brush = flowingRainbowBrush,
                size = Size(earnedWidth, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                alpha = 1.0f
            )
        }
    }
}

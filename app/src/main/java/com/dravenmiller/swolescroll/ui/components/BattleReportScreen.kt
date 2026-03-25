package com.dravenmiller.swolescroll.features.logworkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleReportScreen(
    improvements: List<com.dravenmiller.swolescroll.model.SkillImprovement>,
    onBackToRewards: () -> Unit, // 👈 Goes back to Victory screen
    onExitQuest: () -> Unit      // 👈 Saves and exits the whole workout
) {
    var startAnimations by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        startAnimations = true
    }

    val reportTitle = when {
        improvements.size >= 3 -> "Training Breakthrough!"
        improvements.size == 2 -> "Skills Increased"
        else -> "Power Gained"
    }

    Scaffold(
        topBar = {
            // We can completely hide the navigation icons now since we have big buttons!
            TopAppBar(
                title = { Text("Battle Report", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color(0xFFFFD700)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = reportTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            improvements.forEachIndexed { index, skill ->
                var triggerThisBar by remember { mutableStateOf(false) }
                LaunchedEffect(startAnimations) {
                    if (startAnimations) {
                        delay(index * 400L)
                        triggerThisBar = true
                    }
                }

                TickingSkillBar(
                    skillName = skill.skillName,
                    old1RM = skill.old1RM,
                    new1RM = skill.new1RM,
                    bonusDamage = skill.bonusDamage,
                    startAnimation = triggerThisBar,
                    prMessage = skill.prMessage
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 👇 Pushes the buttons to the very bottom of the screen!
            Spacer(modifier = Modifier.weight(1f))

            // 🔘 THE MASSIVE NAVIGATION BUTTONS 🔘
            AnimatedVisibility(visible = startAnimations) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToRewards,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD700)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)),
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Text("Rewards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onExitQuest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                        modifier = Modifier.weight(1f).height(64.dp)
                    ) {
                        Text("Finish Quest", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TickingSkillBar(
    skillName: String,
    old1RM: Float,
    new1RM: Float,
    bonusDamage: Int, // We leave this here in case you want to use it later!
    prMessage: String, // 👈 NEW: Accepts the generated PR string
    startAnimation: Boolean
) {
    val animated1RM by animateFloatAsState(
        targetValue = if (startAnimation) new1RM else old1RM,
        animationSpec = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
        label = "ticking_numbers"
    )

    val barFraction = animated1RM % 1f
    val barProgress = if (barFraction == 0f && animated1RM > old1RM) 1f else barFraction

    var lastWholePound by remember { mutableStateOf(old1RM.toInt()) }
    var triggerMiniPop by remember { mutableStateOf(false) }

    LaunchedEffect(animated1RM) {
        if (animated1RM.toInt() > lastWholePound) {
            lastWholePound = animated1RM.toInt()
            triggerMiniPop = true
            delay(150)
            triggerMiniPop = false
        }
    }

    val textScale by animateFloatAsState(
        targetValue = if (triggerMiniPop) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "text_bounce"
    )

    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(animated1RM) {
        if (animated1RM == new1RM && new1RM > old1RM) {
            showConfetti = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. SKILL NAME
        Text(
            text = skillName,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // 2. LEVEL UP TEXT & ARROW
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("Lvl %.1f", animated1RM),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4CAF50), // Green
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer {
                    scaleX = textScale
                    scaleY = textScale
                }
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
        }

        // 3. THE PR MESSAGE (e.g. "+5 lbs PR")
        Text(
            text = prMessage,
            style = MaterialTheme.typography.labelMedium,
            color = Color.LightGray,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        // 4. THE PROGRESS BAR
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.DarkGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = barProgress)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(colors = listOf(Color(0xFF2E7D32), Color(0xFF4CAF50))))
                )
            }

            if (showConfetti) {
                val particleY = remember { Animatable(0f) }
                val particleAlpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    launch { particleY.animateTo(-60f, tween(800, easing = FastOutSlowInEasing)) }
                    launch { delay(400); particleAlpha.animateTo(0f, tween(400)) }
                }
                Row(
                    modifier = Modifier.graphicsLayer { translationY = particleY.value; alpha = particleAlpha.value },
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("✨", fontSize = 20.sp); Text("LEVEL UP!", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold); Text("✨", fontSize = 20.sp)
                }
            }
        }
    }
}

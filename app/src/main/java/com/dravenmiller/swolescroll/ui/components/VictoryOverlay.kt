package com.dravenmiller.swolescroll.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dravenmiller.swolescroll.model.SkillImprovement
import com.dravenmiller.swolescroll.util.RpgMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat

@Composable
fun VictoryOverlay(
    workoutName: String,
    startingXp: Int,
    gainedXp: Int,
    lastWeekHp: Int,
    improvements: List<SkillImprovement> = emptyList(),
    onContinue: () -> Unit,
    onOpenBattleReport: () -> Unit
) {
    val animatedXp = remember { Animatable(startingXp.toFloat()) }
    var isAnimationDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        animatedXp.animateTo(
            targetValue = (startingXp + gainedXp).toFloat(),
            animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
        )
        delay(1500)
        isAnimationDone = true
    }

    val currentAnimXp = animatedXp.value.toInt()
    val currentLevel = RpgMath.calculateLevel(currentAnimXp)
    val levelBaseXp = RpgMath.xpRequiredForLevel(currentLevel)
    val nextLevelXp = RpgMath.xpRequiredForLevel(currentLevel + 1)
    val startingLevel = RpgMath.calculateLevel(startingXp)
    val didLevelUp = currentLevel > startingLevel
    val progress = if (nextLevelXp > levelBaseXp) {
        (currentAnimXp - levelBaseXp).toFloat() / (nextLevelXp - levelBaseXp).toFloat()
    } else 0f

    val isOvertime = gainedXp > lastWeekHp
    val isDefeated = gainedXp == lastWeekHp
    val hordeStatusText = when {
        isOvertime -> "Horde Crushed!"
        isDefeated -> "Horde Defeated!"
        else -> "Horde Retreated"
    }
    val bonusDamage = if (isOvertime) gainedXp - lastWeekHp else 0

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 2f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow_radius"
    )

    val popScale by animateFloatAsState(
        targetValue = if (didLevelUp) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f), label = "pop_scale"
    )

    val levelColor by animateColorAsState(
        targetValue = if (didLevelUp) Color(0xFFFFD700) else Color.White,
        animationSpec = tween(300), label = "level_color"
    )

    Dialog(
        onDismissRequest = { /* Prevent dismissing */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.85f)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. QUEST COMPLETE
                Text(
                    text = "QUEST COMPLETE",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // 2. BIG GLOWING LEVEL UP (Main Character Moment!)
                if (didLevelUp) {
                    Text(
                        text = "LEVEL UP!",
                        style = MaterialTheme.typography.displayLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(color = Color(0xFFFFD700), blurRadius = glowRadius)
                        ),
                        color = levelColor,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .graphicsLayer { scaleX = popScale; scaleY = popScale }
                            .padding(vertical = 16.dp)
                    )
                }

                // 3. XP GAINED
                Text(
                    text = "+${NumberFormat.getIntegerInstance().format(gainedXp)} XP",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFF4CAF50), // Green
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(32.dp))

                // 4. LEVEL & PROGRESS BAR
                Text(
                    text = "Level $currentLevel",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Box(contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        color = Color(0xFF2196F3),
                        trackColor = Color.DarkGray,
                        strokeCap = StrokeCap.Round
                    )

                    if (didLevelUp) {
                        val particleY = remember { Animatable(0f) }
                        val particleAlpha = remember { Animatable(1f) }
                        LaunchedEffect(currentLevel) {
                            launch { particleY.animateTo(-80f, tween(1200, easing = FastOutSlowInEasing)) }
                            launch { delay(600); particleAlpha.animateTo(0f, tween(600)) }
                        }
                        Row(
                            modifier = Modifier.graphicsLayer { translationY = particleY.value; alpha = particleAlpha.value },
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            Text("✨", fontSize = 28.sp); Text("🌟", fontSize = 36.sp); Text("✨", fontSize = 28.sp)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 5. OPTIONAL SMALLER TEXT (Horde Status & Bonus)
                Text(
                    text = hordeStatusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOvertime) MaterialTheme.colorScheme.error else Color.LightGray,
                    fontWeight = FontWeight.Bold
                )

                if (bonusDamage > 0) {
                    Text(
                        text = "+${NumberFormat.getIntegerInstance().format(bonusDamage)} bonus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(48.dp))

                // ⚔️ BATTLE REPORT BUTTON
                AnimatedVisibility(visible = isAnimationDone && improvements.isNotEmpty()) {
                    val reportTitle = when {
                        improvements.size >= 3 -> "Training Breakthrough!"
                        improvements.size == 2 -> "Skills Increased"
                        else -> "Power Gained"
                    }
                    OutlinedButton(
                        onClick = onOpenBattleReport,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD700)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)),
                        modifier = Modifier.fillMaxWidth().height(80.dp).padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Battle Report:\n$reportTitle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                AnimatedVisibility(visible = isAnimationDone) {
                    Button(
                        onClick = onContinue,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            text = "Continue Quest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

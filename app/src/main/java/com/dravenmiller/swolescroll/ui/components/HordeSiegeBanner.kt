package com.dravenmiller.swolescroll.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dravenmiller.swolescroll.util.MonsterRoster
import java.text.NumberFormat

@Composable
fun HordeSiegeBanner(
    domMuscle: String,
    lastWeekVolume: Int,
    currentSessionVolume: Int,
    isArenaExpanded: Boolean,
    hordeRoster: List<Int>, // 👈 NEW: It accepts the list from the Shared Brain!
    onToggleArena: () -> Unit
) {
    val isOvertime = currentSessionVolume > lastWeekVolume
    val remainingHordeHp = (lastWeekVolume - currentSessionVolume).coerceAtLeast(0)

    val safeLastWeekVolume = lastWeekVolume.coerceAtLeast(1)
    val hordePercentage = (remainingHordeHp.toFloat() / safeLastWeekVolume.toFloat()).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "HordePulse")
    val overtimeColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFD700),
        targetValue = MaterialTheme.colorScheme.error,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "OvertimeColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOvertime) 8.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .animateContentSize()
            .clickable { onToggleArena() }
    ) {
        val hordeRoster = remember(domMuscle) { MonsterRoster.getHordeLineup(domMuscle) }

        if (!isArenaExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enemy Horde Stats",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Horde",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isArenaExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val groundHeight = h * 0.35f
                        val horizon = h - groundHeight
                        val pillarWidth = 24.dp.toPx()

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))
                            ),
                            size = Size(w, horizon)
                        )
                        drawRect(
                            color = Color(0xFF4E342E),
                            topLeft = Offset(0f, horizon),
                            size = Size(w, groundHeight)
                        )

                        val pillarColor = Color(0xFF795548)
                        val pillarShadow = Color(0xFF3E2723)

                        drawRect(color = pillarColor, topLeft = Offset(0f, 0f), size = Size(pillarWidth, h))
                        drawRect(color = pillarShadow, topLeft = Offset(pillarWidth - 10f, 0f), size = Size(10f, h))

                        drawRect(color = pillarColor, topLeft = Offset(w - pillarWidth, 0f), size = Size(pillarWidth, h))
                        drawRect(color = pillarShadow, topLeft = Offset(w - pillarWidth, 0f), size = Size(10f, h))
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)) {
                        val textDisplayColor = if (isOvertime) overtimeColor else Color.White

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isOvertime) "HORDE CRUSHED!" else "$domMuscle Horde",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = textDisplayColor
                            )
                            Text(
                                text = if (isOvertime) "MAX DMG!" else "$remainingHordeHp / $lastWeekVolume",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textDisplayColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { hordePercentage },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = if (isOvertime) textDisplayColor else Color(0xFFE53935),
                            trackColor = Color.Black.copy(alpha = 0.5f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Damage Dealt: ${NumberFormat.getIntegerInstance().format(currentSessionVolume)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE1F5FE)
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).padding(bottom = 12.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        hordeRoster.forEachIndexed { index, imgRes ->
                            val xOffset = ((index - 5.5f) * 26).dp
                            val yOffset = if (index % 2 == 0) 0.dp else (-16).dp

                            Image(
                                painter = painterResource(imgRes),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).offset(x = xOffset, y = yOffset).zIndex(if (index % 2 == 0) 1f else 0f)
                            )
                        }
                    }
                }
            }
        }
    }
}

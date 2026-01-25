package com.dravenmiller.swolescroll.features.logworkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.util.OneRepMaxCalculator

@Composable
fun ExerciseHistoryDialog(
    exerciseName: String,
    history: List<WorkoutExercise>,
    onDismiss: () -> Unit
) {
    val estimatedMax = remember(history) {
        history.maxOfOrNull { entry ->
            entry.sets.maxOfOrNull { set ->
                OneRepMaxCalculator.getSmart1RM(set.weight, set.reps)
            } ?: 0.0
        } ?: 0.0
    }

    val zones = remember(estimatedMax) {
        if (estimatedMax > 0) OneRepMaxCalculator.getTrainingZones(estimatedMax) else emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        // Main Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                // 1. We add padding to the BOX, not the content.
                // This gives the "DrawBehind" canvas room to draw shadows *outside* the paper.
                .padding(12.dp)
                .scrollBackground() // 📜 Apply the 3D Scroll Logic
                .padding(horizontal = 32.dp) // Content padding (stay off the edges)
                .padding(top = 50.dp, bottom = 50.dp) // Content padding (stay off the rolls)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exerciseName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF3E2723),
                            fontWeight = FontWeight.Bold
                        )
                        if (estimatedMax > 0.0) {
                            Text(
                                text = "Est. 1RM: ${estimatedMax.toInt()} lbs",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8D6E63), // Darker earth tone for subtext
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF5D4037))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- ZONES ---
                if (zones.isNotEmpty()) {
                    Text(
                        "Recommended Starting Weights",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        zones.forEach { zone ->
                            RecommendationCard(zone = zone, modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color(0xFF8D6E63).copy(alpha = 0.3f)
                    )
                }

                // --- HISTORY LIST ---
                Text(
                    "History Logs",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (history.isEmpty()) {
                    Text("No history found.", color = Color(0xFF5D4037))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(history) { entry -> HistoryItem(entry) }
                    }
                }
            }
        }
    }
}
// 📜 Matte Parchment Scroll Background (Concave Top/Bottom + Matched Shadows) Emphasize "History" for Past Lifts
fun Modifier.scrollBackground(): Modifier = this.drawBehind {
    // --- Config ---
    val rollHeight = 44.dp.toPx()
    val rollExtension = 12.dp.toPx()
    val curveDepth = 8.dp.toPx() // Depth of the "hourglass" curve on sides
    val endCurveDepth = 6.dp.toPx() // 👈 NEW: Depth of the curve on Top/Bottom edges

    // --- Palette ---
    val paperColor = Color(0xFFFDF5E6)
    val parchmentShadow = Color(0xFFDCC8A0)
    val parchmentHighlight = Color(0xFFFFFDF9)

    // Shadow Config
    val shadowOffset = 6.dp.toPx()
    val dropShadowColor = Color.Black.copy(alpha = 0.25f)

    // --- SHARED GEOMETRY ---
    val left = -rollExtension
    val right = size.width + rollExtension
    val midX = size.width / 2
    val midY_Top = rollHeight / 2

    // 1. DEFINE TOP ROLL PATH (Concave on ALL sides)
    val topRollPath = Path().apply {
        moveTo(left, 0f)

        // Top Edge: Curve Down (Concave)
        quadraticBezierTo(midX, endCurveDepth, right, 0f)

        // Right Side: Curve In (Hourglass)
        quadraticBezierTo(right - curveDepth, midY_Top, right, rollHeight)

        // Bottom Edge of Top Roll: Straight (Connects to paper)
        lineTo(left, rollHeight)

        // Left Side: Curve In (Hourglass)
        quadraticBezierTo(left + curveDepth, midY_Top, left, 0f)
        close()
    }

    // 2. DEFINE BOTTOM ROLL PATH (Concave on ALL sides)
    val bottomTopY = size.height - rollHeight
    val midY_Bottom = size.height - (rollHeight / 2)

    val bottomRollPath = Path().apply {
        moveTo(left, bottomTopY)
        lineTo(right, bottomTopY)

        // Right Side: Curve In
        quadraticBezierTo(right - curveDepth, midY_Bottom, right, size.height)

        // Bottom Edge: Curve Up (Concave)
        quadraticBezierTo(midX, size.height - endCurveDepth, left, size.height)

        // Left Side: Curve In
        quadraticBezierTo(left + curveDepth, midY_Bottom, left, bottomTopY)
        close()
    }

    // --- DRAWING PHASE ---

    // 1. DROP SHADOWS (Drawn first so they are behind)
    // We translate the canvas to draw the same path shifted down
    withTransform({ translate(top = -shadowOffset/2) }) {
        drawPath(path = topRollPath, color = dropShadowColor)
    }
    withTransform({ translate(top = shadowOffset) }) {
        drawPath(path = bottomRollPath, color = dropShadowColor)
    }

    // 2. MAIN PAPER BODY
    drawRect(
        color = paperColor,
        topLeft = Offset(0f, rollHeight * 0.8f),
        size = Size(size.width, size.height - (rollHeight * 1.6f))
    )

    // Paper Texture/Grain
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                parchmentShadow.copy(alpha = 0.4f),
                Color.Transparent,
                parchmentShadow.copy(alpha = 0.4f)
            ),
            startY = rollHeight,
            endY = size.height - rollHeight
        ),
        topLeft = Offset(0f, rollHeight),
        size = Size(size.width, size.height - (rollHeight * 2))
    )

    // 3. DRAW THE ROLLS
    // Matte Gradient (Dark -> Light -> Dark)
    val cylinderGradient = Brush.horizontalGradient(
        0.0f to parchmentShadow,
        0.2f to paperColor,
        0.5f to parchmentHighlight,
        0.8f to paperColor,
        1.0f to parchmentShadow,
        startX = -rollExtension,
        endX = size.width + rollExtension
    )

    drawPath(path = topRollPath, brush = cylinderGradient)
    drawPath(path = bottomRollPath, brush = cylinderGradient)

    // 4. INNER TUCK SHADOWS
    // Top Tuck
    drawRect(
        brush = Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha=0.15f), Color.Transparent)),
        topLeft = Offset(0f, rollHeight),
        size = Size(size.width, 12.dp.toPx())
    )
    // Bottom Tuck
    drawRect(
        brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha=0.15f))),
        topLeft = Offset(0f, size.height - rollHeight - 12.dp.toPx()),
        size = Size(size.width, 12.dp.toPx())
    )
}

// 🎯 Recommendation Card (Darker Theme)
@Composable
fun RecommendationCard(zone: OneRepMaxCalculator.TrainingTarget, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFEBE9).copy(alpha = 0.5f) // Subtle overlay
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8D6E63).copy(alpha=0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(zone.type, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center, color = Color(0xFF5D4037))
            Text("${zone.weight.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
            Text(zone.repRange, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

// History Item (Darker Theme)
@Composable
fun HistoryItem(entry: WorkoutExercise) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFEFEBE9).copy(alpha = 0.5f), MaterialTheme.shapes.small)
        .padding(8.dp)
    ) {
        val safeType = entry.exercise.type ?: ExerciseType.STRENGTH
        Text(text = "Sets: ${entry.sets.size}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)

        if (safeType == ExerciseType.CARDIO) {
            val totalDist = entry.sets.sumOf { it.distance ?: 0.0 }
            val totalTime = entry.sets.sumOf { it.time ?: 0 }
            val min = totalTime / 60
            val sec = totalTime % 60
            val timeStr = String.format("%d:%02d", min, sec)
            val distStr = String.format("%.2f", totalDist).removeSuffix("0").removeSuffix(".")
            val isStairs = entry.exercise.name.contains("Stair", true)
            val unit = if (isStairs) "stairs" else "mi"
            val speedValue = if (isStairs) (if (totalTime/60.0 > 0) totalDist / (totalTime/60.0) else 0.0) else (if (totalTime/3600.0 > 0) totalDist / (totalTime/3600.0) else 0.0)
            val speedUnit = if (isStairs) "SPM" else "mph"
            Text(text = "Total: $distStr $unit in $timeStr avg speed: ${String.format("%.2f", speedValue)} $speedUnit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF3E2723))
        } else {
            entry.sets.forEach { set ->
                val text = when (safeType) {
                    ExerciseType.LoadedCarry -> "${set.weight} lbs for ${set.distance ?: 0.0} yds"
                    ExerciseType.ISOMETRIC -> "${set.weight} lbs for ${set.timeFormatted()}"
                    ExerciseType.TWENTY_ONES -> "${set.weight} lbs (21s)"
                    else -> "${set.weight} lbs x ${set.reps}"
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp), color = Color(0xFF3E2723))
            }
        }
    }
}

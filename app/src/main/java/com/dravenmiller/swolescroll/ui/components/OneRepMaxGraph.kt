package com.dravenmiller.swolescroll.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.util.OneRepMaxCalculator

enum class GraphMode {
    SMART, EPLEY, BRZYCKI, LOMBARDI, OCONNER, BEST
}

@Composable
fun OneRepMaxGraph(
    history: List<WorkoutExercise>,
    modifier: Modifier = Modifier.fillMaxWidth().height(250.dp),
    selectedMode: GraphMode = GraphMode.SMART,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (history.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data to graph yet!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // 1. DETECT TYPE & CONFIGURATION
    val latestEntry = history.first()
    val type = latestEntry.exercise.type ?: ExerciseType.STRENGTH

    // "Formula Mode" is only for Lifting (Strength/21s).
    // Cardio, Iso, and Carry use raw values.
    val useFormulas = (type == ExerciseType.STRENGTH || type == ExerciseType.TWENTY_ONES)

    // 2. PREPARE DATA 📊
    data class PlotPoint(
        val date: Long,
        val value: Double, // The main value to graph (Speed, Weight, or Selected 1RM)
        val estimates: OneRepMaxCalculator.Estimates? = null // Stored ONLY if we are in Formula Mode
    )

    val dataPoints = remember(history, type, selectedMode) {
        history.mapNotNull { entry ->
            val date = entry.workoutDate

            when {
                // A. CARDIO (Speed)
                type.isCardio -> {
                    val dist = entry.sets.sumOf { it.distance ?: 0.0 }
                    val time = entry.sets.sumOf { it.time ?: 0 }
                    val value = if (time > 0) {
                        if (type == ExerciseType.STAIRS) (dist / (time / 60.0)) // Steps/Min
                        else (dist / (time / 3600.0)) // MPH
                    } else 0.0
                    if (value > 0) PlotPoint(date, value) else null
                }

                // B. LOADED CARRY (Volume: Weight * Distance)
                type == ExerciseType.LoadedCarry -> {
                    // Find the single set with the highest volume
                    val bestSet = entry.sets.maxByOrNull { (it.weight * (it.distance ?: 0.0)) }
                    if (bestSet != null) {
                        val volume = bestSet.weight * (bestSet.distance ?: 0.0)
                        if (volume > 0) PlotPoint(date, volume) else null
                    } else null
                }

                // C. ISOMETRIC (Raw Max Weight)
                type == ExerciseType.ISOMETRIC -> {
                    val maxWeight = entry.sets.maxOfOrNull { it.weight } ?: 0.0
                    if (maxWeight >= 0) PlotPoint(date, maxWeight) else null
                }

                // D. STRENGTH (1RM Formulas)
                else -> {
                    val bestSet = entry.sets.maxByOrNull { it.weight }
                    if (bestSet != null && bestSet.weight > 0) {
                        val est = OneRepMaxCalculator.getAllEstimates(bestSet.weight, bestSet.reps)
                        val heroValue = when (selectedMode) {
                            GraphMode.SMART -> est.smart
                            GraphMode.EPLEY -> est.epley
                            GraphMode.BRZYCKI -> est.brzycki
                            GraphMode.LOMBARDI -> est.lombardi
                            GraphMode.OCONNER -> est.oconner
                            GraphMode.BEST -> est.best
                        }
                        PlotPoint(date, heroValue, est)
                    } else null
                }
            }
        }.sortedBy { it.date }
    }

    if (dataPoints.isEmpty()) return

    // 3. SCALING 📐
    val maxVal = dataPoints.maxOf {
        if (useFormulas && it.estimates != null) it.estimates.best else it.value
    } * 1.05

    val minVal = dataPoints.minOf {
        if (useFormulas && it.estimates != null) it.estimates.lombardi else it.value
    } * 0.95

    val yRange = (maxVal - minVal).coerceAtLeast(1.0)
    val minDate = dataPoints.first().date
    val maxDate = dataPoints.last().date
    val xRange = (maxDate - minDate).coerceAtLeast(1L)

    // 4. DRAWING 🎨
    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height

        fun getX(date: Long): Float = ((date - minDate) / xRange.toFloat()) * width
        fun getY(value: Double): Float {
            val normalized = (value - minVal) / yRange
            return height - (normalized.toFloat() * height)
        }

        // --- GRID ---
        val steps = 4
        for (i in 0..steps) {
            val yRatio = i / steps.toFloat()
            val y = height - (yRatio * height)
            val value = minVal + (yRatio * yRange)

            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )

            // Y-Label Logic
            val label = when {
                type == ExerciseType.STAIRS -> "${value.toInt()}" // Steps are whole numbers
                type.isCardio -> String.format("%.1f", value)     // Speed needs decimals
                type == ExerciseType.LoadedCarry -> {
                    // Format large volume numbers (e.g. 2500 -> 2.5k)
                    val vol = value.toInt()
                    if (vol >= 1000) String.format("%.1fk", vol / 1000.0) else "$vol"
                }
                else -> "${value.toInt()}"                        // Weight is usually whole number
            }

            drawContext.canvas.nativeCanvas.drawText(
                label, 0f, y - 5f,
                Paint().apply { color = android.graphics.Color.GRAY; textSize = 30f }
            )
        }

        fun drawLineFromPoints(extractor: (PlotPoint) -> Double, color: Color, alpha: Float = 1f, stroke: Float = 3.dp.toPx()) {
            val path = Path()
            dataPoints.forEachIndexed { index, point ->
                val x = getX(point.date)
                val y = getY(extractor(point))
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = alpha), style = Stroke(width = stroke, cap = StrokeCap.Round))
        }

        // --- GHOST LINES (Only for Strength/Formulas) ---
        if (useFormulas) {
            val ghostColor = Color.Gray
            if (selectedMode != GraphMode.EPLEY) drawLineFromPoints({ it.estimates?.epley ?: 0.0 }, ghostColor, 0.3f, 2f)
            if (selectedMode != GraphMode.BRZYCKI) drawLineFromPoints({ it.estimates?.brzycki ?: 0.0 }, ghostColor, 0.3f, 2f)
            if (selectedMode != GraphMode.LOMBARDI) drawLineFromPoints({ it.estimates?.lombardi ?: 0.0 }, ghostColor, 0.3f, 2f)
            if (selectedMode != GraphMode.OCONNER) drawLineFromPoints({ it.estimates?.oconner ?: 0.0 }, ghostColor, 0.3f, 2f)
        }

        // --- HERO LINE ---
        drawLineFromPoints({ it.value }, lineColor, 1f, 6.dp.toPx())

        // --- HERO DOTS ---
        dataPoints.forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(getX(point.date), getY(point.value))
            )
        }
    }
}

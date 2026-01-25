package com.dravenmiller.swolescroll.ui.components

import android.R.attr.path
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.swolescroll.features.stats.GraphPoint // Make sure this imports correctly
import com.dravenmiller.swolescroll.util.OneRepMaxCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GraphMode {
    SMART, EPLEY, BRZYCKI, LOMBARDI, OCONNER, BEST
}
@Composable
fun OneRepMaxGraph(
    data: List<GraphPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedMode: GraphMode = GraphMode.SMART
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text("Not enough data to graph yet!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // 1. Calculate Scales 📐
    // We add a little "padding" (5%) to the top/bottom so the line doesn't touch the edge
    val maxVal = data.maxOf { it.estimates.best }
    val minVal = data.minOf { it.estimates.lombardi }
    val yRange = (maxVal - minVal).coerceAtLeast(10.0) // Avoid divide by zero

    val minDate = data.minOf { it.date }
    val maxDate = data.maxOf { it.date }
    val xRange = (maxDate - minDate).coerceAtLeast(1L)

    val heroColor = MaterialTheme.colorScheme.primary

    // 2. Formatters 📅
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp) // Standard graph height
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height

        // 3. Coordinate Transformer Function 🤖
        // Converts "315 lbs on Jan 1st" -> "Pixel(x: 50, y: 200)"
        fun getX(date: Long): Float {
            return ((date - minDate) / xRange.toFloat()) * width
        }

        fun getY(value: Double): Float {
            // Invert Y because Canvas (0,0) is top-left
            val normalized = (value - minVal) / yRange
            return height - (normalized.toFloat() * height)
        }

        // 4. Draw Grid Lines (Background) 📏
        val steps = 4
        for (i in 0..steps) {
            val yRatio = i / steps.toFloat()
            val y = height - (yRatio * height)
            val value = minVal + (yRatio * yRange)

            // Draw Line
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )

            // Draw Text Label
            drawContext.canvas.nativeCanvas.drawText(
                "${value.toInt()}",
                0f,
                y - 5f,
                Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                }
            )
        }

        // 2. HELPER TO DRAW A LINE ✍️
        fun drawTrendLine(
            extractor: (OneRepMaxCalculator.Estimates) -> Double,
            color: Color,
            alpha: Float = 1f,
            strokeWidth: Float = 3.dp.toPx()
        ) {
            val path = Path()
            data.forEachIndexed { index, point ->
                val x = getX(point.date)
                val y = getY(extractor(point.estimates))
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // 3. DRAW GHOST LINES 👻 (The ones not selected)
        // We draw them thin and faint
        if (selectedMode != GraphMode.EPLEY) drawTrendLine({ it.epley }, Color.Gray, 0.5f, 2f)
        if (selectedMode != GraphMode.BRZYCKI) drawTrendLine({ it.brzycki }, Color.Gray, 0.5f, 2f)
        if (selectedMode != GraphMode.LOMBARDI) drawTrendLine({ it.lombardi }, Color.Gray, 0.5f, 2f)
        if (selectedMode != GraphMode.OCONNER) drawTrendLine({ it.oconner }, Color.Gray, 0.5f, 2f)

        // 4. DRAW HERO LINE 🦸‍♂️ (The selected one)
        val heroExtractor: (OneRepMaxCalculator.Estimates) -> Double = when(selectedMode) {
            GraphMode.SMART -> { it -> it.smart }
            GraphMode.EPLEY -> { it -> it.epley }
            GraphMode.BRZYCKI -> { it -> it.brzycki }
            GraphMode.LOMBARDI -> { it -> it.lombardi }
            GraphMode.OCONNER -> { it -> it.oconner }
            GraphMode.BEST -> { it -> it.best }
        }

        drawTrendLine(heroExtractor, heroColor, 1f, 5.dp.toPx()) // Thicker!

        // Draw Dots for Hero Line
        data.forEach { point ->
            drawCircle(
                color = heroColor,
                radius = 4.dp.toPx(),
                center = Offset(getX(point.date), getY(heroExtractor(point.estimates)))
            )
        }
    }
}
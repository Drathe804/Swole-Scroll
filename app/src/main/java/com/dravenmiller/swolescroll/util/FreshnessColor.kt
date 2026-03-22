package com.dravenmiller.swolescroll.util

import androidx.compose.ui.graphics.Color
import java.util.concurrent.TimeUnit

object FreshnessUtils {

    // 1. Define your RPG Colors
    val ColorFresh = Color(0xFF4CAF50)      // Green (Recent)
    val ColorStale = Color(0xFFFFC107)      // Amber (Week old)
    val ColorAncient = Color(0xFF2196F3)    // Blue (Month old / Never)
    val ColorNever = Color(0xFF9E9E9E)      // Grey (Never touched)

    // 2. The Logic Function
    fun getFreshnessColor(lastPerformed: Long?): Color {
        if (lastPerformed == null || lastPerformed == 0L) return ColorNever

        val diff = System.currentTimeMillis() - lastPerformed
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            days < 7 -> ColorFresh       // < 1 Week
            days < 30 -> ColorStale      // 1-4 Weeks
            else -> ColorAncient         // > 1 Month
        }
    }

    fun getFreshnessLabel(lastPerformed: Long?): String {
        // 1. Handle the "New Quest" Case (Grey/Null)
        if (lastPerformed == null || lastPerformed == 0L) {
            return "New Quest ⚔️"
        }

        // 2. Calculate the Difference
        val diff = System.currentTimeMillis() - lastPerformed
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        // 3. Return Human-Readable Time
        return when {
            days == 0L -> " Today"
            days == 1L -> " Yesterday"
            days < 7 -> " $days days"       // e.g. "4 days"
            days < 30 -> " ${days / 7} weeks" // e.g. "2 wks"
            days < 365 -> " ${days / 30} months" // e.g. "5 mos"
            else -> " ${days / 365} years"    // e.g. "1 yrs"
        }
    }
}


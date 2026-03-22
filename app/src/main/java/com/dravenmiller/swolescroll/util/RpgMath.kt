package com.dravenmiller.swolescroll.util

object RpgMath {
    /**
     * Calculates the player's level based on a Triangular Curve.
     * Level 1 = 1k, Level 2 = 3k, Level 3 = 6k, etc.
     */
    fun calculateLevel(totalXp: Int): Int {
        if (totalXp < 1000) return 0
        // The reversed algebraic formula for a triangular sequence
        return ((kotlin.math.sqrt(1.0 + (totalXp / 125.0)) - 1.0) / 2.0).toInt()
    }

    /**
     * Calculates exactly how much total XP is needed to reach a specific level.
     */
    fun xpRequiredForLevel(level: Int): Int {
        if (level <= 0) return 0
        return (level * (level + 1) / 2) * 1000
    }
}

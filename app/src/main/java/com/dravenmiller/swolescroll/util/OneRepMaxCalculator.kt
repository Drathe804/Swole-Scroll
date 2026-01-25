package com.dravenmiller.swolescroll.util // Adjust package if needed

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

object OneRepMaxCalculator {

    data class Estimates(
        val smart: Double,
        val epley: Double,
        val brzycki: Double,
        val lombardi: Double,
        val oconner: Double,
        val best: Double // The highest of all 3 (Ego Mode)
    )

    fun getAllEstimates(weight: Double, reps: Int): Estimates {
        if (reps == 0) return Estimates(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        if (reps == 1) return Estimates(weight, weight, weight, weight, weight, weight)

        val epley = calculateEpley(weight, reps)
        val brzycki = calculateBrzycki(weight, reps)
        val lombardi = calculateLombardi(weight, reps)
        val oconner = calculateOConner(weight, reps)

        // The Smart Logic we wrote earlier
        val smart = when {
            reps <= 5 -> brzycki
            reps <= 12 -> epley
            else -> lombardi
        }

        // The "Best" logic (Cherry-pick the highest number)
        val best = max(epley, max(brzycki, lombardi))

        return Estimates(smart, epley, brzycki, lombardi, oconner, best)
    }

    data class TrainingTarget(
        val type: String,
        val weight: Double,
        val repRange: String
    )

    fun getTrainingZones(trueOneRepMax: Double): List<TrainingTarget> {
        // Round to nearest 5 lbs
        fun snap(w: Double) = (round(w / 5) * 5)

        // 1. Calculate "Training Max" (TM)
        // Standard powerlifting rule: Base your math off 90% of your true max.
        // This prevents burnout and ensures clean form.
        val trainingMax = trueOneRepMax * 0.90

        return listOf(
            // Strength: 85% of TM (approx 77% of True Max)
            // Example: Max 354 -> TM 318 -> Strength Rec 270 lbs
            TrainingTarget("Strength", snap(trainingMax * 0.85), "3-5 reps"),

            // Growth: 70% of TM (approx 63% of True Max)
            TrainingTarget("Growth", snap(trainingMax * 0.70), "8-12 reps"),

            // Endurance: 50% of TM (approx 45% of True Max)
            TrainingTarget("Endurance", snap(trainingMax * 0.50), "15+ reps")
        )
    }

    /**
     * 🧠 THE SMART SELECT ALGORITHM
     * Automatically picks the most accurate formula based on rep range.
     */
    fun getSmart1RM(weight: Double, reps: Int): Double {
        if (reps == 1) return weight // No math needed for a true max
        if (reps == 0) return 0.0

        return when {
            // 1-5 Reps: BRZYCKI
            // Why: Mathematically stricter. At low reps, you want accuracy, not inflation.
            reps <= 5 -> calculateBrzycki(weight, reps)

            // 6-12 Reps: EPLEY
            // Why: The Gold Standard for hypertrophy. Best for standard lifting sets.
            reps <= 12 -> calculateEpley(weight, reps)

            // 13+ Reps: LOMBARDI
            // Why: Epley tends to overestimate at high reps. Lombardi scales better for endurance.
            else -> calculateLombardi(weight, reps)
        }
    }

    // --- THE FORMULAS 🧪 ---

    // 1. Epley: w * (1 + r/30)
    fun calculateEpley(weight: Double, reps: Int): Double {
        return weight * (1 + (reps / 30.0))
    }

    // 2. Brzycki: w / (1.0278 - 0.0278 * r)
    fun calculateBrzycki(weight: Double, reps: Int): Double {
        // Prevent division by zero if someone enters crazy reps (like 37)
        if (reps >= 37) return weight
        return weight / (1.0278 - (0.0278 * reps))
    }

    // 3. Lombardi: w * r^0.10
    fun calculateLombardi(weight: Double, reps: Int): Double {
        return weight * reps.toDouble().pow(0.10)
    }

    // 4. O'Conner: w * (1 + r/40)
    // (A more conservative version of Epley, often used for easy sets)
    fun calculateOConner(weight: Double, reps: Int): Double {
        return weight * (1 + (reps / 40.0))
    }
}

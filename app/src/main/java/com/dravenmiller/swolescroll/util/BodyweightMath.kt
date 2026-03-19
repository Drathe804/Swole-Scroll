package com.dravenmiller.swolescroll.util

object BodyweightMath {

    /**
     * Returns the percentage of bodyweight moved based on the exercise name.
     * 1.0 = 100%, 0.65 = 65%, etc.
     */
    fun getMultiplier(exerciseName: String): Double {
        val name = exerciseName.lowercase().trim()

        return when {
            // 💪 UPPER BODY PUSH
            name.contains("push-up") || name.contains("push up") -> 0.64 // Biomechanical standard (~64%)
            name.contains("knee push") -> 0.49
            name.contains("dip") -> 1.0 // You lift your whole body
            name.contains("planche") -> 1.0

            // 💪 UPPER BODY PULL
            name.contains("pull-up") || name.contains("pull up") -> 1.0
            name.contains("chin-up") || name.contains("chin up") -> 1.0
            name.contains("muscle-up") || name.contains("muscle up") -> 1.0
            name.contains("row") && name.contains("inverted") -> 0.60

            // 🦵 LEGS
            name.contains("squat") -> 1.0 // Air squats move full body
            name.contains("lunge") -> 1.0
            name.contains("step-up") || name.contains("step up") -> 1.0
            name.contains("pistol") -> 1.0
            name.contains("jump") -> 1.0

            // 🐢 CORE (Variable, but usually treated as bodyweight load)
            name.contains("crunch") -> 0.15 // Usually negligible for "tonnage"
            name.contains("sit-up") || name.contains("sit up") -> 0.1
            name.contains("leg raise") -> 0.30 // Leg weight ~30% of body

            // Default for any other exercise marked "isBodyweight"
            else -> 1.0
        }
    }
}

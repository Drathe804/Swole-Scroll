package com.dravenmiller.swolescroll.data

import com.dravenmiller.swolescroll.model.Exercise

object QuestCombos {
    // ⚔️ SYNERGISTS (Same push/pull plane - Good for burnouts/giant sets)
    val synergies = mapOf(
        "Chest" to listOf("Triceps", "Front Delt", "Shoulders"),
        "Back" to listOf("Biceps", "Rear Delt", "Lats", "Traps"),
        "Quads" to listOf("Hamstrings", "Calves", "Glutes", "Legs"),
        "Hamstrings" to listOf("Glutes", "Calves", "Quads", "Legs"),
        "Shoulders" to listOf("Side Delt", "Front Delt", "Rear Delt", "Triceps"),
        "Biceps" to listOf("Forearms"),
        "Triceps" to listOf("Side Delt", "Chest")
    )

    // 🛡️ ANTAGONISTS (Opposite muscles - Good for saving time/Boss mode)
    val antagonists = mapOf(
        "Chest" to listOf("Back", "Lats", "Biceps", "Rear Delt"),
        "Back" to listOf("Chest", "Triceps", "Front Delt"),
        "Quads" to listOf("Hamstrings", "Glutes"),
        "Hamstrings" to listOf("Quads"),
        "Biceps" to listOf("Triceps"),
        "Triceps" to listOf("Biceps"),
        "Shoulders" to listOf("Lats", "Back")
    )

    // 🧗 GRIP HEAVY MOVEMENTS (Rule #2: Don't pair these unless necessary)
    private val gripKeywords = listOf("Deadlift", "Row", "Pull", "Chin", "Carry", "Farmer", "Shrug", "Curl")

    fun isGripHeavy(exercise: Exercise): Boolean {
        // If it's a back or biceps move with these keywords, it's grip heavy.
        // We exclude pressing movements even if they have "press" in name.
        if (exercise.muscleGroup == "Back" || exercise.muscleGroup == "Biceps" || exercise.muscleGroup == "Lats" || exercise.muscleGroup == "Traps" || exercise.type == com.dravenmiller.swolescroll.model.ExerciseType.LoadedCarry) {
            return gripKeywords.any { exercise.name.contains(it, true) }
        }
        return false
    }

    // Helper to check compatibility based on rules
    fun areCompatible(ex1: Exercise, ex2: Exercise, allowGripFailure: Boolean = false): Boolean {
        // Rule #2: Grip Check
        if (!allowGripFailure && isGripHeavy(ex1) && isGripHeavy(ex2)) {
            return false // Too much grip work!
        }

        // Check for Synergy OR Antagonist synergy
        val isSynergist = synergies[ex1.muscleGroup]?.contains(ex2.muscleGroup) == true || synergies[ex2.muscleGroup]?.contains(ex1.muscleGroup) == true
        val isAntagonist = antagonists[ex1.muscleGroup]?.contains(ex2.muscleGroup) == true || antagonists[ex2.muscleGroup]?.contains(ex1.muscleGroup) == true

        return isSynergist || isAntagonist
    }
}

package com.dravenmiller.swolescroll.model

enum class ExerciseType(
    val displayName: String,
    val isCardio: Boolean,     // 👈 Helps us swap the UI controls
    val canBeUnilateral: Boolean // 👈 Tells us whether to show the checkbox
) {
    // Standard Lifting
    STRENGTH("Strength", false, true),

    // Special Types
    LoadedCarry("Loaded Carry", false, false), // Farmers carry usually isn't "Left Arm Only" in the same way
    ISOMETRIC("Isometric", false, false),
    TWENTY_ONES("21s", false, true),

    // The Cardio Family (Flat list for DB, grouped by 'isCardio' property)
    CARDIO("General Cardio", true, false),
    TREADMILL("Treadmill", true, false),
    STAIRS("Stairs", true, false),
    ROWING("Rowing", true, false)
}

object DistanceUni {
    const val MILES = "mi"
    const val YARDS = "yd"
    const val STAIRS = "stairs"
}
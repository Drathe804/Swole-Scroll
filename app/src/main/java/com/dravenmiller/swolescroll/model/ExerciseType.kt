package com.dravenmiller.swolescroll.model

enum class ExerciseType {
    STRENGTH, // Weight x Reps (Default)
    CARDIO, // Distance x Time
    ISOMETRIC, // Weight x Time
    LoadedCarry, // Weight x Distance
    TWENTY_ONES,
}

object DistanceUni {
    const val MILES = "mi"
    const val YARDS = "yd"
    const val STAIRS = "stairs"
}
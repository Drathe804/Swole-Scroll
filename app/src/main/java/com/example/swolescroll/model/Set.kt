package com.example.swolescroll.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Set(
    val id: String = UUID.randomUUID().toString(),
    val weight: Double,
    val reps: Int,
    val rpe: Int? = null, // Optional for warm-up sets
    val isWarmup: Boolean = false, // Handy for tracking volume later

    val distance: Double? = 0.0, // Optional for cardio exercises
    val time: Int = 0, // Stored in seconds
): Parcelable {
    fun timeFormatted(): String {
        if (time == 0) return ""
        val minutes = time / 60
        val seconds = time % 60

        return String.format("%d:%02d", minutes, seconds)
    }
    // In model/Set.kt
    fun getSpeed(): String {
        val safeDistance = distance ?: 0.0

        if (time == 0 || safeDistance == 0.0) return "0 mph"

        // Math: Distance / (Seconds / 3600) = Miles Per Hour
        val hours = time / 3600.0
        val mph = safeDistance / hours

        return "%.1f mph".format(mph)
    }

}
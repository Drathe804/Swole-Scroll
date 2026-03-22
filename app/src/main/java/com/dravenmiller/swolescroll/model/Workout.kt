package com.dravenmiller.swolescroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dravenmiller.swolescroll.util.BodyweightMath
import java.util.UUID

// @Entity = "Make a table for this in the database"
@Entity(tableName = "workout_table")
data class Workout(
    // @PrimaryKey = "This is the unique ID for the row"
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val date: Long = System.currentTimeMillis(),
    val name: String = "New Workout",
    val durationMinutes: Int = 0,

    // Our Converter handles this complex list automatically!
    val exercises: List<WorkoutExercise> = emptyList(),

    // Notes
    val notes: String = "",
    val isQuest: Boolean = false
)
data class WorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val exercise: Exercise,
    val sets: List<Set>,
    val note: String? = null,
    val workoutDate: Long = 0,
    val supersetId: String? = null
)

fun WorkoutExercise.calculateTotalVolume(userWeight: Double): Int {
    val multiplier = if (exercise.isSingleSide) 2 else 1
    val bwMultiplier = BodyweightMath.getMultiplier(exercise.name)

    return sets.sumOf { set ->
        val effectiveWeight = if (exercise.isBodyweight) {
            (userWeight * bwMultiplier) + set.weight
        } else {
            set.weight
        }

        val d = set.distance ?: 0.0
        set.time ?: 0
        val safeType = exercise.type ?: ExerciseType.STRENGTH

        when (safeType) {
            ExerciseType.STRENGTH -> (effectiveWeight * set.reps * multiplier).toInt()
            ExerciseType.ISOMETRIC -> 0
            ExerciseType.LoadedCarry -> (effectiveWeight * d * multiplier).toInt()
            ExerciseType.TWENTY_ONES -> {
                val rawVol = (effectiveWeight * set.reps * multiplier)
                ((rawVol * 2)/3).toInt()
            }
            else -> 0
        }
    }
}
fun WorkoutExercise.calculateTotalTUT(): Int {
    return sets.sumOf { it.time ?: 0 }
}
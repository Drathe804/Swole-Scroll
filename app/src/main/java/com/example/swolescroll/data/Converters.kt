package com.example.swolescroll.data

import androidx.room.TypeConverter
import com.example.swolescroll.model.WorkoutExercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // Tells Room: "If you see a List<WorkoutExercise>, here is how to save it as a String"
    @TypeConverter
    fun fromWorkoutExerciseList(value: List<WorkoutExercise>): String {
        val type = object : TypeToken<List<WorkoutExercise>>() {}.type
        return gson.toJson(value, type)
    }

    // Tells Room: "If you see a String, here is how to turn it back into a List<WorkoutExercise>"
    @TypeConverter
    fun toWorkoutExerciseList(value: String): List<WorkoutExercise> {
        val type = object : TypeToken<List<WorkoutExercise>>() {}.type
        return gson.fromJson(value, type)
    }
}

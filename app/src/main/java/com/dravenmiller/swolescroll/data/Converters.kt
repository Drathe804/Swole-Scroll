package com.dravenmiller.swolescroll.data

import androidx.room.TypeConverter
import com.dravenmiller.swolescroll.model.SkillImprovement
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // --- WORKOUT EXERCISE CONVERTERS ---
    @TypeConverter
    fun fromWorkoutExerciseList(value: List<WorkoutExercise>): String {
        val type = object : TypeToken<List<WorkoutExercise>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toWorkoutExerciseList(value: String): List<WorkoutExercise> {
        val type = object : TypeToken<List<WorkoutExercise>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    // --- ⚔️ SKILL IMPROVEMENT CONVERTERS ⚔️ ---
    @TypeConverter
    fun fromSkillImprovementList(value: List<SkillImprovement>): String {
        val type = object : TypeToken<List<SkillImprovement>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toSkillImprovementList(value: String): List<SkillImprovement> {
        val type = object : TypeToken<List<SkillImprovement>>() {}.type
        return gson.fromJson(value, type) ?: emptyList() // The ?: emptyList() is a safety net!
    }
}

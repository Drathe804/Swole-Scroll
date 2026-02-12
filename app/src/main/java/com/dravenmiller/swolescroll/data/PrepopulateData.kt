package com.dravenmiller.swolescroll.data

import com.dravenmiller.swolescroll.model.Exercise
import com.dravenmiller.swolescroll.model.ExerciseType

object PrepopulateData {
    val defaultExercises = listOf(
        // 🏃 CARDIO
        Exercise(name = "Treadmill", muscleGroup = "Cardio", type = ExerciseType.TREADMILL),
        Exercise(name = "Stair Master", muscleGroup = "Cardio", type = ExerciseType.STAIRS),
        Exercise(name = "Rowing Machine", muscleGroup = "Cardio", type = ExerciseType.ROWING),
        Exercise(name = "Elliptical", muscleGroup = "Cardio", type = ExerciseType.CARDIO),
        Exercise(name = "Stationary Bike", muscleGroup = "Cardio", type = ExerciseType.CARDIO),
        Exercise(name = "Jump Rope", muscleGroup = "Cardio", type = ExerciseType.CARDIO),

        // 💪 CHEST (Stays mostly "Chest")
        Exercise(name = "Bench Press (Barbell)", muscleGroup = "Chest", type = ExerciseType.STRENGTH),
        Exercise(name = "Bench Press (Dumbbell)", muscleGroup = "Chest", type = ExerciseType.STRENGTH),
        Exercise(name = "Incline Bench Press", muscleGroup = "Chest", type = ExerciseType.STRENGTH),
        Exercise(name = "Pec Fly (Cable)", muscleGroup = "Chest", type = ExerciseType.STRENGTH),
        Exercise(name = "Push Ups", muscleGroup = "Chest", type = ExerciseType.STRENGTH),
        Exercise(name = "Dips", muscleGroup = "Chest", type = ExerciseType.STRENGTH),

        // 🐢 BACK (Split for "Necromancer" Logic)
        // Note: Deadlift is now Lower Back to enable the safe "Necromancer" logic
        Exercise(name = "Deadlift", muscleGroup = "Lower Back", type = ExerciseType.STRENGTH),
        Exercise(name = "Pull Up", muscleGroup = "Lats", type = ExerciseType.STRENGTH),
        Exercise(name = "Lat Pulldown", muscleGroup = "Lats", type = ExerciseType.STRENGTH),
        Exercise(name = "Seated Cable Row", muscleGroup = "Traps", type = ExerciseType.STRENGTH), // or Upper Back
        Exercise(name = "Bent Over Row (Barbell)", muscleGroup = "Lats", type = ExerciseType.STRENGTH),
        Exercise(name = "Single Arm Row (Dumbbell)", muscleGroup = "Lats", type = ExerciseType.STRENGTH, isSingleSide = true),

        // 🦵 LEGS (Split for "Colossus" Logic)
        Exercise(name = "Squat (Barbell)", muscleGroup = "Quads", type = ExerciseType.STRENGTH),
        Exercise(name = "Leg Press", muscleGroup = "Quads", type = ExerciseType.STRENGTH),
        Exercise(name = "Walking Lunges", muscleGroup = "Glutes", type = ExerciseType.STRENGTH, isSingleSide = true),
        Exercise(name = "Bulgarian Split Squat", muscleGroup = "Glutes", type = ExerciseType.STRENGTH, isSingleSide = true),
        Exercise(name = "Romanian Deadlift (RDL)", muscleGroup = "Hamstrings", type = ExerciseType.STRENGTH),
        Exercise(name = "Leg Extension", muscleGroup = "Quads", type = ExerciseType.STRENGTH),
        Exercise(name = "Hamstring Curl", muscleGroup = "Hamstrings", type = ExerciseType.STRENGTH),
        Exercise(name = "Calf Raises", muscleGroup = "Calves", type = ExerciseType.STRENGTH),

        // Stabilizers (For Deadlift Day Support)
        Exercise(name = "Hip Adduction (Machine)", muscleGroup = "Adductors", type = ExerciseType.STRENGTH),
        Exercise(name = "Hip Abduction (Machine)", muscleGroup = "Abductors", type = ExerciseType.STRENGTH),

        // 🥥 SHOULDERS (Split for "Spartan" Logic)
        Exercise(name = "Overhead Press (Barbell)", muscleGroup = "Front Delt", type = ExerciseType.STRENGTH),
        Exercise(name = "Shoulder Press (Dumbbell)", muscleGroup = "Front Delt", type = ExerciseType.STRENGTH),
        Exercise(name = "Lateral Raise", muscleGroup = "Side Delt", type = ExerciseType.STRENGTH),
        Exercise(name = "Face Pull", muscleGroup = "Rear Delt", type = ExerciseType.STRENGTH),
        Exercise(name = "Arnold Press", muscleGroup = "Front Delt", type = ExerciseType.STRENGTH),
        Exercise(name = "Shrugs", muscleGroup = "Traps", type = ExerciseType.STRENGTH),
        Exercise(name = "Rear Delt Fly", muscleGroup = "Rear Delt", type = ExerciseType.STRENGTH),

        // 💪 ARMS
        Exercise(name = "Bicep Curl (Barbell)", muscleGroup = "Biceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Bicep Curl (Dumbbell)", muscleGroup = "Biceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Hammer Curl", muscleGroup = "Biceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Preacher Curl", muscleGroup = "Biceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Tricep Pushdown", muscleGroup = "Triceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Skullcrusher", muscleGroup = "Triceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Overhead Tricep Ext", muscleGroup = "Triceps", type = ExerciseType.STRENGTH),
        Exercise(name = "Bicep 21s", muscleGroup = "Biceps", type = ExerciseType.TWENTY_ONES),

        // ✨ SPECIAL / FINISHERS
        // Note: Specific group "Forearms" prevents Farmer Carries from auto-adding as a main Back lift
        Exercise(name = "Farmer Carries", muscleGroup = "Forearms", type = ExerciseType.LoadedCarry),
        Exercise(name = "Suitcase Carry", muscleGroup = "Core", type = ExerciseType.LoadedCarry, isSingleSide = true),

        // 🧱 CORE / ISOMETRIC
        Exercise(name = "Plank", muscleGroup = "Core", type = ExerciseType.ISOMETRIC),
        Exercise(name = "Wall Sit", muscleGroup = "Quads", type = ExerciseType.ISOMETRIC),
        Exercise(name = "Cable Crunch", muscleGroup = "Core", type = ExerciseType.STRENGTH),
        Exercise(name = "Leg Raise", muscleGroup = "Core", type = ExerciseType.STRENGTH),
        Exercise(name = "Dead Bug", muscleGroup = "Core", type = ExerciseType.STRENGTH)
    )
}

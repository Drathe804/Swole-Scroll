package com.dravenmiller.swolescroll.features.quests

import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.data.QuestCombos
import com.dravenmiller.swolescroll.model.Exercise
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.WorkoutExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

// 1. THE STYLE (Goal)
enum class QuestDifficulty {
    SCOUT,   // Endurance
    RAID,    // Hypertrophy
    BOSS     // Strength
}

// 2. THE DURATION (Time)
enum class DurationLevel {
    EASY,    // Quick
    NORMAL,  // Standard
    HARD     // Long
}

data class QuestResult(val title: String, val exercises: List<WorkoutExercise>, val notes: String)

class QuestManager(private val db: AppDatabase) {

    suspend fun generateQuest(style: QuestDifficulty): QuestResult = withContext(Dispatchers.IO) {
        // --- PART 1: ANALYZE HISTORY ---
        val lastWorkout = db.workoutDao().getLastWorkout()
        val allWorkouts = db.workoutDao().getAllWorkouts().firstOrNull() ?: emptyList()

        val usageMap = mutableMapOf<String, Long>()
        allWorkouts.forEach { workout ->
            workout.exercises.forEach { we ->
                val existing = usageMap[we.exercise.name] ?: 0L
                if (workout.date > existing) {
                    usageMap[we.exercise.name] = workout.date
                }
            }
        }

        val lastExercises = lastWorkout?.exercises ?: emptyList()
        val lastMuscleGroup = lastExercises.firstOrNull()?.exercise?.muscleGroup ?: "Full Body"
        val hadDeadlift = lastExercises.any { it.exercise.name.contains("Deadlift", true) }
        val hadCardio = lastExercises.any { it.exercise.type?.isCardio == true }

        val targetMuscle = when {
            hadDeadlift -> "Legs"
            lastMuscleGroup == "Legs" -> "Chest"
            lastMuscleGroup == "Chest" -> "Cardio"
            hadCardio -> "Core"
            lastMuscleGroup == "Core" -> "Shoulders"
            lastMuscleGroup == "Shoulders" -> "Back"
            lastMuscleGroup == "Back" -> "Deadlift"
            else -> "Chest"
        }

        val allExercises = db.exerciseDao().getAllExercisesList()
        val questList = mutableListOf<Exercise>()
        var questName = ""

        // --- PART 2: THE MATRIX SETUP ---
        val profile = db.userDao().getUserProfileOneShot()
        val durationStr = profile?.defaultDifficulty ?: "NORMAL"
        val duration = try {
            DurationLevel.valueOf(durationStr)
        } catch (e: Exception) { DurationLevel.NORMAL }

        val targetCount = when (style) {
            QuestDifficulty.SCOUT -> if (duration == DurationLevel.EASY) 3 else if (duration == DurationLevel.NORMAL) 5 else 7
            QuestDifficulty.RAID -> if (duration == DurationLevel.EASY) 3 else if (duration == DurationLevel.NORMAL) 5 else 7
            QuestDifficulty.BOSS -> if (duration == DurationLevel.EASY) 2 else if (duration == DurationLevel.NORMAL) 4 else 6
        }

        val styleNotes = when (style) {
            QuestDifficulty.SCOUT -> "Class: Ranger (Endurance)\nTarget: High Reps, Short Rest (30s).\nPerform as a Circuit."
            QuestDifficulty.RAID -> "Class: Barbarian (Hypertrophy)\nTarget: 8-12 Reps, Moderate Rest (90s).\nFocus on the pump."
            QuestDifficulty.BOSS -> "Class: Paladin (Strength)\nTarget: 1-5 Reps, Long Rest (3-5 min).\nLift Heavy. Protect your CNS."
        }

        // --- PART 3: GENERATE WORKOUT (Exercises Picked HERE) 🧠 ---
        // ⚠️ CRITICAL: This MUST run before we try to group them!
        when (targetMuscle) {
            "Deadlift" -> {
                questName = "Boss Battle: The Necromancer"
                val mainLift = allExercises.find { it.name.contains("Deadlift", true) && !it.name.contains("Romanian") }
                    ?: allExercises.firstOrNull { it.muscleGroup == "Lower Back" }
                if (mainLift != null) questList.add(mainLift)

                val accessoryCount = (targetCount - 2).coerceAtLeast(1)
                val safeAccessories = allExercises.filter {
                    (it.muscleGroup == "Hamstrings" || it.muscleGroup == "Glutes" || it.muscleGroup == "Legs") &&
                            it.id != mainLift?.id &&
                            it.type?.isCardio != true &&
                            !it.name.contains("Deadlift", true) &&
                            !it.name.contains("Row", true) &&
                            !it.name.contains("Squat", true)
                }.shuffled().take(accessoryCount)
                questList.addAll(safeAccessories)

                val stabilizer = allExercises.filter {
                    (it.muscleGroup == "Adductors" || it.muscleGroup == "Abductors" || it.name.contains("Hip", true)) && it.type?.isCardio != true
                }.shuffled().firstOrNull()
                if (stabilizer != null && isUniqueEnough(stabilizer, questList)) questList.add(stabilizer)

                val finisher = allExercises.filter {
                    (it.type == ExerciseType.TWENTY_ONES || it.name.contains("21", true)) && (it.muscleGroup == "Biceps" || it.muscleGroup == "Hamstrings")
                }.shuffled().firstOrNull() ?: allExercises.filter { it.muscleGroup == "Core" && !it.name.contains("Carry", true) }.shuffled().firstOrNull()
                if (finisher != null && isUniqueEnough(finisher, questList)) questList.add(finisher)
            }

            "Shoulders" -> {
                questName = "Boss Battle: The Spartan"
                val press = allExercises.filter {
                    (it.muscleGroup == "Front Delt" || it.muscleGroup == "Shoulders") &&
                            (it.name.contains("Press", true) || it.name.contains("Overhead", true)) && it.type?.isCardio != true
                }.sortedBy { usageMap[it.name] ?: 0L }.firstOrNull()
                if (press != null) questList.add(press)

                val side = allExercises.filter { (it.muscleGroup == "Side Delt" || it.name.contains("Lateral", true)) && it.type?.isCardio != true }.shuffled().firstOrNull()
                if (side != null && isUniqueEnough(side, questList)) questList.add(side)

                val rear = allExercises.filter { (it.muscleGroup == "Rear Delt" || it.name.contains("Face Pull", true)) && it.type?.isCardio != true }.shuffled().firstOrNull()
                if (rear != null && isUniqueEnough(rear, questList)) questList.add(rear)

                while (questList.size < targetCount) {
                    val filler = allExercises.filter { (it.muscleGroup == "Traps" || it.muscleGroup == "Side Delt" || it.muscleGroup == "Shoulders") && isUniqueEnough(it, questList) && it.type?.isCardio != true }.shuffled().firstOrNull()
                    if (filler != null) questList.add(filler) else break
                }
            }

            "Cardio" -> {
                questName = "Scout Mission: Cardio & Carry"
                val count = targetCount.coerceAtMost(4)
                val cardio = allExercises.filter { it.type?.isCardio == true }.shuffled().take(count)
                questList.addAll(cardio)
                addCarry(questList, allExercises)
            }

            "Core" -> {
                questName = "Quest: Iron Core"
                val coreEx = allExercises.filter { it.muscleGroup == "Core" }.shuffled().sortedBy { usageMap[it.name] ?: 0L }.take(targetCount)
                questList.addAll(coreEx)
            }

            "Legs" -> {
                questName = "Boss Battle: The Colossus"
                val legExercises = allExercises.filter {
                    (it.muscleGroup == "Legs" || it.muscleGroup == "Quads" || it.muscleGroup == "Hamstrings" || it.muscleGroup == "Glutes" || it.muscleGroup == "Calves") &&
                            it.type?.isCardio != true && !it.name.contains("Deadlift", true)
                }
                fun getStale(filter: (Exercise) -> Boolean): List<Exercise> = legExercises.filter(filter).shuffled().sortedBy { usageMap[it.name] ?: 0L }

                val compounds = getStale { it.name.contains("Squat", true) || it.name.contains("Leg Press", true) || it.name.contains("Hack", true) || it.name.contains("Lunge", true) }
                val hams = getStale { it.name.contains("Curl", true) || it.name.contains("Glute", true) || it.name.contains("Hip Thrust", true) }
                val quads = getStale { it.name.contains("Extension", true) || it.name.contains("Step", true) || it.name.contains("Split", true) }
                val details = getStale { it.muscleGroup == "Calves" || it.muscleGroup == "Adductors" }

                if (compounds.isNotEmpty()) questList.add(compounds.first())
                if (hams.isNotEmpty()) questList.add(hams.first())
                if (quads.isNotEmpty()) questList.add(quads.first())

                val remainingCount = targetCount - questList.size
                val leftovers = (details + compounds.drop(1) + hams.drop(1) + quads.drop(1)).filter { isUniqueEnough(it, questList) }.shuffled().take(remainingCount)
                questList.addAll(leftovers)

                if (style == QuestDifficulty.BOSS) {
                    addCarry(questList, allExercises)
                } else {
                    val burnout = allExercises.find { (it.name.contains("Wall Sit", true) || it.type == ExerciseType.ISOMETRIC) && it.muscleGroup == "Legs" }
                    if (burnout != null && isUniqueEnough(burnout, questList)) questList.add(burnout) else addCarry(questList, allExercises)
                }
            }

            else -> {
                val secondaryMuscle = getSynergist(targetMuscle)
                questName = "Quest: $targetMuscle" + if (secondaryMuscle.isNotEmpty()) " & $secondaryMuscle" else ""

                val keyWord = if (targetMuscle == "Back") "Row" else "Press"
                val mainLift = allExercises.filter {
                    (it.muscleGroup == targetMuscle || (targetMuscle == "Back" && (it.muscleGroup == "Lats" || it.muscleGroup == "Traps"))) &&
                            it.name.contains(keyWord, true) && it.type?.isCardio != true
                }.sortedBy { usageMap[it.name] ?: 0L }.firstOrNull() ?: allExercises.firstOrNull { it.muscleGroup == targetMuscle && it.type?.isCardio != true }

                if (mainLift != null) questList.add(mainLift)

                val primaries = allExercises.filter {
                    (it.muscleGroup == targetMuscle || (targetMuscle == "Back" && (it.muscleGroup == "Lats" || it.muscleGroup == "Traps"))) &&
                            (mainLift == null || it.id != mainLift.id) && it.type?.isCardio != true
                }.shuffled().sortedBy { usageMap[it.name] ?: 0L }

                val secondaries = if (secondaryMuscle.isNotEmpty()) {
                    allExercises.filter { it.muscleGroup == secondaryMuscle && it.type?.isCardio != true }.shuffled().sortedBy { usageMap[it.name] ?: 0L }
                } else emptyList()

                var pIndex = 0; var sIndex = 0; var nextIsPrimary = false
                while (questList.size < targetCount) {
                    val candidate = if (nextIsPrimary) {
                        if (pIndex < primaries.size) primaries[pIndex++] else null
                    } else {
                        if (secondaries.isNotEmpty() && sIndex < secondaries.size) secondaries[sIndex++] else if (pIndex < primaries.size) primaries[pIndex++] else null
                    }

                    if (candidate != null && isUniqueEnough(candidate, questList)) {
                        questList.add(candidate)
                        if (secondaries.isNotEmpty()) nextIsPrimary = !nextIsPrimary
                    } else {
                        if (pIndex >= primaries.size && sIndex >= secondaries.size) break
                        if (secondaries.isNotEmpty()) nextIsPrimary = !nextIsPrimary
                    }
                }

                if (style == QuestDifficulty.BOSS) {
                    addCarry(questList, allExercises)
                } else {
                    val finisherTarget = if (secondaryMuscle.isNotEmpty()) secondaryMuscle else targetMuscle
                    val finisher = allExercises.filter {
                        (it.type == ExerciseType.TWENTY_ONES || it.name.contains("21", true)) &&
                                (it.muscleGroup == finisherTarget || it.muscleGroup == "Biceps") && it.type?.isCardio != true
                    }.firstOrNull()
                    if (finisher != null && isUniqueEnough(finisher, questList)) questList.add(finisher) else addCarry(questList, allExercises)
                }
            }
        }

        // --- PART 4: THE MATRIX APPLICATION (Grouping Supersets) 🧠 ---

        // 1. DEFINE STRATEGY
        var setsToCreate = 0
        var giantSetsToCreate = 0
        var bossImmunityActive = false
        var allowGripFailure = false

        when (style) {
            QuestDifficulty.SCOUT -> {
                allowGripFailure = true
                when (duration) {
                    DurationLevel.EASY -> setsToCreate = 1
                    DurationLevel.NORMAL -> setsToCreate = 2
                    DurationLevel.HARD -> { giantSetsToCreate = 1; setsToCreate = 1 }
                }
            }
            QuestDifficulty.RAID -> {
                when (duration) {
                    DurationLevel.EASY -> setsToCreate = 0
                    DurationLevel.NORMAL -> setsToCreate = 1
                    DurationLevel.HARD -> setsToCreate = 2
                }
            }
            QuestDifficulty.BOSS -> {
                bossImmunityActive = true
                when (duration) {
                    DurationLevel.EASY -> setsToCreate = 0
                    DurationLevel.NORMAL -> setsToCreate = 1
                    DurationLevel.HARD -> setsToCreate = 1
                }
            }
        }

        val finalWorkoutExercises = mutableListOf<WorkoutExercise>()
        val processedIndexes = mutableSetOf<Int>()

        var currentIndex = if (bossImmunityActive) 1 else 0

        // Handle Boss Immunity
        if (bossImmunityActive && questList.isNotEmpty()) {
            finalWorkoutExercises.add(WorkoutExercise(id = UUID.randomUUID().toString(), exercise = questList[0], sets = emptyList()))
            processedIndexes.add(0)
        }

        // A. Giant Sets
        while (giantSetsToCreate > 0 && currentIndex + 2 < questList.size) {
            val size = if (currentIndex + 3 < questList.size && Random.nextBoolean()) 4 else 3
            val candidates = (currentIndex until currentIndex + size).map { questList[it] }

            if (QuestCombos.areCompatible(candidates[0], candidates[1], allowGripFailure)) {
                // 🔑 SHARED DUNGEON ID FOR EVERYONE
                val dungeonId = UUID.randomUUID().toString()

                // Add all as peers in the dungeon
                candidates.forEachIndexed { i, ex ->
                    finalWorkoutExercises.add(
                        WorkoutExercise(
                            id = UUID.randomUUID().toString(),
                            exercise = ex,
                            sets = emptyList(),
                            supersetId = dungeonId, // 👈 EVERYONE GETS THE ID
                            note = if (i == 0) "🏰 Giant Set Leader" else "🔗 Link ${i+1}"
                        )
                    )
                }

                (currentIndex until currentIndex + size).forEach { processedIndexes.add(it) }
                currentIndex += size
                giantSetsToCreate--
            } else {
                currentIndex++
            }
        }

        // B. Supersets
        while (setsToCreate > 0 && currentIndex + 1 < questList.size) {
            if (processedIndexes.contains(currentIndex)) { currentIndex++; continue }
            if (processedIndexes.contains(currentIndex + 1)) { currentIndex += 2; continue }

            val ex1 = questList[currentIndex]
            val ex2 = questList[currentIndex + 1]

            if (QuestCombos.areCompatible(ex1, ex2, allowGripFailure)) {
                // 🔑 SHARED DUNGEON ID FOR BOTH
                val dungeonId = UUID.randomUUID().toString()

                // Boss (Now Dark)
                finalWorkoutExercises.add(WorkoutExercise(
                    id = UUID.randomUUID().toString(),
                    exercise = ex1,
                    sets = emptyList(),
                    supersetId = dungeonId // 👈 BOSS GETS ID
                ))
                // Minion (Now Dark)
                finalWorkoutExercises.add(WorkoutExercise(
                    id = UUID.randomUUID().toString(),
                    exercise = ex2,
                    sets = emptyList(),
                    supersetId = dungeonId, // 👈 MINION GETS ID
                    note = "⚡ Superset Link"
                ))

                processedIndexes.add(currentIndex)
                processedIndexes.add(currentIndex + 1)
                currentIndex += 2
                setsToCreate--
            } else {
                currentIndex++
            }
        }

        // C. Leftovers (Straight Sets)
        questList.forEachIndexed { index, exercise ->
            if (!processedIndexes.contains(index)) {
                finalWorkoutExercises.add(WorkoutExercise(id = UUID.randomUUID().toString(), exercise = exercise, sets = emptyList()))
            }
        }

        val sortedFinalList = finalWorkoutExercises.sortedBy { we -> questList.indexOfFirst { it.id == we.exercise.id } }

        return@withContext QuestResult(questName, sortedFinalList, styleNotes)
    }

    private fun addCarry(list: MutableList<Exercise>, all: List<Exercise>) {
        val carry = all.find { it.type == ExerciseType.LoadedCarry && !it.name.contains("Suitcase") } ?: all.find { it.name.contains("Carry", true) }
        if (carry != null && !list.any { it.name.contains("Carry", true) } && carry.type?.isCardio != true) {
            list.add(carry)
        }
    }

    private fun getSynergist(muscle: String): String {
        return when (muscle) {
            "Chest" -> "Triceps"
            "Back" -> "Biceps"
            else -> ""
        }
    }

    private fun isUniqueEnough(candidate: Exercise, currentList: List<Exercise>): Boolean {
        if (currentList.any { it.id == candidate.id }) return false
        val candidateRoot = candidate.name.substringBefore("(").trim().lowercase()
        val rootConflict = currentList.any { existing ->
            val existingRoot = existing.name.substringBefore("(").trim().lowercase()
            existing.muscleGroup == candidate.muscleGroup && (candidateRoot == existingRoot || candidateRoot.contains(existingRoot) || existingRoot.contains(candidateRoot))
        }
        if (rootConflict) return false
        val triggers = listOf("Press", "Squat", "Lunge", "Raise", "Extension", "Curl", "Dip", "Row", "Pull", "Chin", "Push", "Crunch", "Sit", "Calf", "Glute", "Abduct", "Adduct", "Fly", "Deadlift", "Bridge", "Thrust")
        val candidateTrigger = triggers.find { candidate.name.contains(it, true) }
        if (candidateTrigger != null) {
            val patternConflict = currentList.any { existing ->
                existing.muscleGroup == candidate.muscleGroup && existing.name.contains(candidateTrigger, true)
            }
            if (patternConflict) return false
        }
        return true
    }
}

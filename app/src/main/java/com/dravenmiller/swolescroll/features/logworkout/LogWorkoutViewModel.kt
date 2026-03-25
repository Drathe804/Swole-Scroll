package com.dravenmiller.swolescroll.features.logworkout

import android.app.Application // Import this!
import android.os.Build
import android.util.Log
import android.util.Log.e
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel // Import this!
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.data.BackupManager // Import your new manager
import com.dravenmiller.swolescroll.model.Draft
import com.dravenmiller.swolescroll.model.Exercise
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.Set
import com.dravenmiller.swolescroll.model.Workout
import com.dravenmiller.swolescroll.model.WorkoutExercise
import com.dravenmiller.swolescroll.util.BodyweightMath
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.collections.sumOf
import kotlinx.coroutines.flow.combine

// Change "ViewModel" to "AndroidViewModel(application)"
class LogWorkoutViewModel(
    private val application: Application, // Needs this to save files
    private val db: AppDatabase // Pass the whole DB for easier access
) : AndroidViewModel(application) {


    val isQuest = mutableStateOf(false)
    var workoutName = mutableStateOf("")
    var addedExercises = mutableStateListOf<WorkoutExercise>()
    var showDialog = mutableStateOf(false)
    var workoutDate = mutableStateOf(System.currentTimeMillis())
    var workoutNote = mutableStateOf("")
    var showResumeDialog = mutableStateOf(false)
    private var pendingDraft: Workout? = null
    val showHistoryDialog = mutableStateOf(false)
    var historyTitle by mutableStateOf("")
    private val _exerciseHistory = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val exerciseHistory = _exerciseHistory.asStateFlow()
    private val _exerciseFreshnessMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val exerciseFreshnessMap = _exerciseFreshnessMap.asStateFlow()
    var currentWorkoutId: String? = null
    var activeDungeonId by mutableStateOf<String?>(null)

    init {
        loadFreshnessHistory()
        checkForDraft()
    }

    // 2. Build the Map (Name -> Last Date)
    private fun loadFreshnessHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val allWorkouts = db.workoutDao().getAllWorkouts().firstOrNull() ?: emptyList()
            val historyMap = mutableMapOf<String, Long>()

            allWorkouts.forEach { workout ->
                workout.exercises.forEach { we ->
                    val currentMax = historyMap[we.exercise.name] ?: 0L
                    if (workout.date > currentMax) {
                        historyMap[we.exercise.name] = workout.date
                    }
                }
            }
            _exerciseFreshnessMap.value = historyMap
        }
    }

    // 🎚️ SIEGE MODE TOGGLE (Saved instantly to device)
    private val prefs = application.getSharedPreferences("SwoleScrollPrefs", android.content.Context.MODE_PRIVATE)
    private val _isSiegeModeEnabled = MutableStateFlow(prefs.getBoolean("siege_mode", true)) // Defaults to TRUE
    val isSiegeModeEnabled = _isSiegeModeEnabled.asStateFlow()

    fun toggleSiegeMode() {
        val newState = !_isSiegeModeEnabled.value
        prefs.edit().putBoolean("siege_mode", newState).apply()
        _isSiegeModeEnabled.value = newState
    }

    val exerciseList = db.exerciseDao().getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBodyWeight = db.userDao().getUserProfile()
        .map { it?.bodyWeight ?: 0.0 } // Default to 0 if not set
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ⚔️ LIFETIME XP (For the Victory Screen calculation)
    val lifetimeVolume = db.workoutDao().getAllWorkouts()
        .combine(userBodyWeight) { workouts, uWeight ->
            var total = 0
            workouts.forEach { workout ->
                workout.exercises.forEach { we ->
                    val type = we.exercise.type ?: ExerciseType.STRENGTH
                    val multiplier = if (we.exercise.isSingleSide) 2 else 1
                    val bwPercentage = BodyweightMath.getMultiplier(we.exercise.name)

                    we.sets.forEach { set ->
                        val w = if (we.exercise.isBodyweight) (uWeight * bwPercentage) + set.weight else set.weight
                        val d = set.distance ?: 0.0
                        val t = set.time ?: 0

                        total += when (type) {
                            ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                            ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                            ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                            ExerciseType.TWENTY_ONES -> (((w * set.reps * multiplier) * 2) / 3).toInt()
                            else -> 0
                        }
                    }
                }
            }
            total
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    // ⚔️ DEFEND THE KINGDOM: MONSTER HP GENERATOR
    val monsterHpMap: StateFlow<Map<String, Int>> = db.workoutDao().getAllWorkouts()
        .combine(userBodyWeight) { workouts, uWeight ->
            val hpMap = mutableMapOf<String, Int>()
            workouts.sortedByDescending { it.date }.forEach { workout ->
                workout.exercises.forEach { we ->
                    val name = we.exercise.name
                    if (!hpMap.containsKey(name)) {
                        var sessionVol = 0
                        val type = we.exercise.type ?: ExerciseType.STRENGTH
                        val multiplier = if (we.exercise.isSingleSide) 2 else 1
                        val bwPercentage = BodyweightMath.getMultiplier(name)

                        we.sets.forEach { set ->
                            val w = if (we.exercise.isBodyweight) (uWeight * bwPercentage) + set.weight else set.weight
                            val d = set.distance ?: 0.0
                            val t = set.time ?: 0

                            sessionVol += when (type) {
                                ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                                ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                                ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                                ExerciseType.TWENTY_ONES -> (((w * set.reps * multiplier) * 2) / 3).toInt()
                                else -> 0
                            }
                        }
                        if (sessionVol > 0) hpMap[name] = sessionVol
                    }
                }
            }
            hpMap
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 🏰 DEFEND THE KINGDOM: HISTORICAL HORDE HP
    val historicalHordeHpMap: StateFlow<Map<String, Int>> = db.workoutDao().getAllWorkouts()
        .combine(userBodyWeight) { workouts, uWeight ->
            val map = mutableMapOf<String, Int>()
            workouts.sortedByDescending { it.date }.forEach { workout ->
                val rawMuscle = workout.exercises.firstOrNull()?.exercise?.muscleGroup ?: return@forEach
                val broadMuscle = getBroadMuscleGroup(rawMuscle) // 👈 Groups "Front Delt" into "Shoulders"

                if (!map.containsKey(broadMuscle)) { // 👈 Save it under the broad faction name!

                    var totalVol = 0
                    workout.exercises.forEach { we ->
                        val type = we.exercise.type ?: ExerciseType.STRENGTH
                        val multiplier = if (we.exercise.isSingleSide) 2 else 1
                        val bwPercentage = BodyweightMath.getMultiplier(we.exercise.name)

                        we.sets.forEach { set ->
                            val w = if (we.exercise.isBodyweight) (uWeight * bwPercentage) + set.weight else set.weight
                            val d = set.distance ?: 0.0
                            val t = set.time ?: 0

                            totalVol += when (type) {
                                ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                                ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                                ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                                ExerciseType.TWENTY_ONES -> (((w * set.reps * multiplier) * 2) / 3).toInt()
                                else -> 0
                            }
                        }
                    }
                    if (totalVol > 0) map[broadMuscle] = totalVol
                }
            }
            map
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 1. SMART PR TRACKER (Totals + Dominant Level) 🏆
    val personalRecords: StateFlow<Map<String, String>> =
        db.workoutDao().getAllWorkouts()
            .map { workouts ->
                val prMap = mutableMapOf<String, String>()

                // Track "Bests" differently for each type
                val maxCardioSpeed = mutableMapOf<String, Double>() // Speed (Dist / Time)
                val maxCarryWeight = mutableMapOf<String, Double>() // Weight
                val maxCarryDist = mutableMapOf<String, Double>()   // Distance (tie-breaker)
                val maxStrengthWeight = mutableMapOf<String, Double>() // Weight

                workouts.forEach { workout ->
                    workout.exercises.forEach { workoutExercise ->
                        val name = workoutExercise.exercise.name
                        val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                        when (type) {
                            ExerciseType.CARDIO -> {
                                val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }

                                if (totalDist > 0 && totalSeconds > 0) {
                                    // 🧠 SMART CHECK: Is this Stairs or Distance?
                                    val isStairs = name.contains("Stair", ignoreCase = true)

                                    if (isStairs) {
                                        // 🪜 STAIRS MATH: Steps per Minute
                                        val minutes = totalSeconds / 60.0
                                        val spm = totalDist / minutes

                                        val currentBest = maxCardioSpeed[name] ?: 0.0
                                        if (spm > currentBest) {
                                            maxCardioSpeed[name] = spm
                                            val niceSpm = String.format("%.1f", spm)
                                            prMap[name] = "$niceSpm stairs/min" // 👈 Explicit unit
                                        }
                                    } else {
                                        // 🏃 RUNNING MATH: Miles per Hour
                                        val hours = totalSeconds / 3600.0
                                        val speed = totalDist / hours

                                        val currentBest = maxCardioSpeed[name] ?: 0.0
                                        if (speed > currentBest) {
                                            maxCardioSpeed[name] = speed
                                            val niceSpeed = String.format("%.2f", speed)
                                            prMap[name] = "$niceSpeed mph" // 👈 Explicit unit
                                        }
                                    }
                                }
                            }

                            ExerciseType.LoadedCarry -> {
                                // 🏋️ LOADED CARRY: Heavier Weight WINS. If Tie, Further Distance WINS.
                                val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
                                val w = bestSet?.weight ?: 0.0
                                val d = bestSet?.distance ?: 0.0

                                val currentMaxW = maxCarryWeight[name] ?: 0.0
                                val currentMaxD = maxCarryDist[name] ?: 0.0

                                val isHeavier = w > currentMaxW
                                val isSameWeightFurther = (w == currentMaxW && d > currentMaxD)

                                if (isHeavier || isSameWeightFurther) {
                                    maxCarryWeight[name] = w
                                    maxCarryDist[name] = d
                                    prMap[name] = "$w lbs for $d yds"
                                }
                            }

                            else -> {
                                // 💪 STRENGTH / ISO / 21s
                                val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
                                val w = bestSet?.weight ?: 0.0
                                val r = bestSet?.reps ?: 0 // Or Time for Isometric

                                val currentMax = maxStrengthWeight[name] ?: 0.0
                                if (w > currentMax) {
                                    maxStrengthWeight[name] = w
                                    if (type == ExerciseType.ISOMETRIC) {
                                        prMap[name] = "$w lbs"
                                    } else {
                                        prMap[name] = "$w lbs x $r" // Simplified string
                                    }
                                }
                            }
                        }
                    }
                }
                prMap.toMap()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    // 🧠 THE FACTION SORTER: Groups sub-muscles into major armies
    fun getBroadMuscleGroup(muscle: String): String {
        val m = muscle.lowercase()
        return when {
            m.contains("chest") || m.contains("pec") -> "Chest"
            m.contains("back") || m.contains("lat") || m.contains("rhomboid") || m.contains("trap") -> "Back"
            m.contains("leg") || m.contains("quad") || m.contains("ham") || m.contains("calf") || m.contains("calves") || m.contains("glute") -> "Legs"
            m.contains("shoulder") || m.contains("delt") -> "Shoulders"
            m.contains("bicep") || m.contains("tricep") || m.contains("arm") -> "Arms"
            m.contains("core") || m.contains("abs") || m.contains("oblique") -> "Core"
            else -> muscle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }



    init {
        checkForDraft()
    }

    val exerciseNotesHistory: StateFlow<Map<String, List<String>>> =
        db.workoutDao().getAllWorkouts()
            .map { workouts ->
                val notesMap = mutableMapOf<String, MutableList<String>>()
                val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())

                workouts.forEach { workout ->
                    val dateStr = dateFormat.format(java.util.Date(workout.date))
                    workout.exercises.forEach { workoutExercise ->
                        if (!workoutExercise.note.isNullOrBlank()) {
                            val entry = "$dateStr: ${workoutExercise.note}"
                            notesMap.getOrPut(workoutExercise.exercise.name) { mutableListOf() }
                                .add(entry)
                        }
                    }
                }
                notesMap
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    fun startDungeon(parentIndex: Int) {
        if (parentIndex < 0 || parentIndex >= addedExercises.size) return
        val parent = addedExercises[parentIndex]

        // 🧠 UPDATED LOGIC:
        // 1. If I am already a Minion (in a dungeon), link the new guy to MY Boss.
        if (parent.supersetId != null) {
            activeDungeonId = parent.supersetId
        }
        // 2. If I am the Boss (Normal), the new guy links to ME.
        // We do NOT change the 'parent'. It stays Normal.
        else {
            activeDungeonId = parent.id
        }

        showDialog.value = true
    }

    // ⚔️ START FRESH DUNGEON (Separate from current)
    fun startNewDungeon_Fresh() {
        // Generate a brand new Key for the upcoming exercise
        activeDungeonId = java.util.UUID.randomUUID().toString()
        showDialog.value = true
    }

    fun loadHistory(targetExerciseId: String, name: String) {
        historyTitle = name
        viewModelScope.launch {
            val allWorkouts = db.workoutDao().getAllWorkouts().first()
            val foundHistory = allWorkouts.flatMap { workout -> workout.exercises }
                //.filter { it.exercise.id == targetExerciseId }
                .filter { it.exercise.name.trim().equals(name.trim(), ignoreCase = true) }
                .map { it.copy(workoutDate = it.workoutDate) }
            _exerciseHistory.value = foundHistory
        }
    }

    private fun checkForDraft() {
        viewModelScope.launch {
            val draft = db.draftDao().getDraft()
            if (draft != null) {
                val savedWorkout = Gson().fromJson(draft.dataJson, Workout::class.java)
                if (savedWorkout.isQuest) {
                    pendingDraft = savedWorkout
                    resumeDraft()
                } else {
                    pendingDraft = savedWorkout
                    showResumeDialog.value = true
                }
            }
        }
    }

    fun resumeDraft() {
        pendingDraft?.let { workout ->
            workoutName.value = workout.name
            workoutDate.value = workout.date
            workoutNote.value = workout.notes ?: ""
            isQuest.value = workout.isQuest
            addedExercises.clear()
            addedExercises.addAll(workout.exercises)
        }
        showResumeDialog.value = false
    }

    fun discardDraft() {
        viewModelScope.launch {
            db.draftDao().clearDraft()
        }
        showResumeDialog.value = false
    }

    fun autoSaveDraft() {
        if (addedExercises.isEmpty() && workoutName.value.isBlank()) return

        viewModelScope.launch {
            val currentState = Workout(
                name = workoutName.value,
                date = workoutDate.value,
                exercises = addedExercises.toList(),
                notes = workoutNote.value
            )
            val json = Gson().toJson(currentState)
            db.draftDao().insertDraft(Draft(dataJson = json))
        }
    }

    fun addExerciseSafe(
        name: String,
        muscleGroup: String,
        isSingleSide: Boolean,
        type: ExerciseType,
        isBodyweight: Boolean,
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val existingExercise = db.exerciseDao().getExerciseByName(cleanName)
            val exerciseToUse = if (existingExercise != null) {
                if (existingExercise.type != null) {
                    val patchedExercise = existingExercise.copy(type = type)
                    db.exerciseDao().updateExercise(patchedExercise)
                    patchedExercise
                } else {
                    existingExercise
                }
            } else {
                val newExercise = Exercise(
                    name = cleanName,
                    muscleGroup = muscleGroup,
                    isSingleSide = isSingleSide,
                    type = type,
                    isBodyweight = isBodyweight
                )
                db.exerciseDao().insertExercise(newExercise)
                newExercise
            }
            val initialSet = Set(
                id = java.util.UUID.randomUUID().toString(),
                weight = 0.0,
                reps = 0,
                distance = 0.0,
                time = 0
            )
            val newWorkoutExercise = WorkoutExercise(
                exercise = exerciseToUse,
                sets = listOf(initialSet)
            )
            addedExercises.add(newWorkoutExercise)
        }
    }

    fun updateExercise(updatedExercise: Exercise) {
        viewModelScope.launch {
            val oldId = updatedExercise.id
            val cleanName = updatedExercise.name.trim()

            // 1. Check if the name we are trying to use ALREADY EXISTS (and isn't us)
            val existingTarget = db.exerciseDao().getExerciseByName(cleanName)

            if (existingTarget != null && existingTarget.id != oldId) {
                // 🚨 MERGE DETECTED: We are renaming "A" to "B", but "B" already exists.
                // Action: Move all history from A to B, then Delete A.

                Log.d(
                    "UpdateExercise",
                    "Merging '${updatedExercise.name}' into existing '${existingTarget.name}'"
                )

                // A. Update History: Find all workouts using the Old Exercise
                val allWorkouts = db.workoutDao().getAllWorkoutsList()

                allWorkouts.forEach { workout ->
                    // Check if this workout contains the exercise we are deleting
                    val hasOldExercise = workout.exercises.any { it.exercise.id == oldId }

                    if (hasOldExercise) {
                        val fixedExercises = workout.exercises.map { we ->
                            if (we.exercise.id == oldId) {
                                // SWAP: Replace the old exercise object with the Target exercise object
                                // We keep the 'type' from the log in case it was specific (like Cardio vs Strength)
                                we.copy(exercise = existingTarget.copy(type = we.exercise.type))
                            } else {
                                we
                            }
                        }
                        // Save the corrected workout
                        db.workoutDao().updateWorkout(workout.copy(exercises = fixedExercises))
                    }
                }

                // B. Delete the "Old" entry since it is now merged
                // We use the ID to ensure we delete the specific row that was being edited
                val exerciseToDelete = exerciseList.value.find { it.id == oldId }
                if (exerciseToDelete != null) {
                    db.exerciseDao().deleteExercise(exerciseToDelete)
                }

            } else {
                // ✏️ NORMAL RENAME: The name is unique. Just update the text.

                // 1. Update the Master Entry in the DB
                db.exerciseDao().updateExercise(updatedExercise)

                // 2. Update History (because workouts store a copy of the name)
                // We need to make sure past logs reflect the new name immediately.
                val allWorkouts = db.workoutDao().getAllWorkoutsList()

                allWorkouts.forEach { workout ->
                    // Check by ID is safer than checking by Name
                    val hasTarget = workout.exercises.any { it.exercise.id == oldId }

                    if (hasTarget) {
                        val updatedList = workout.exercises.map { we ->
                            if (we.exercise.id == oldId) {
                                // Update the embedded exercise object with the new info
                                we.copy(exercise = updatedExercise)
                            } else {
                                we
                            }
                        }
                        db.workoutDao().updateWorkout(workout.copy(exercises = updatedList))
                    }
                }
            }

            // Refresh freshness map to reflect changes immediately
            loadFreshnessHistory()
        }
    }


    // ---------------------------------------------------------
    // ✅ FIXED: EDITING & SAVING LOGIC (Uses 'db' correctly)
    // ---------------------------------------------------------

    // 2. LOAD FUNCTION
    fun initializeForEdit(workoutId: String) {
        viewModelScope.launch {
            // FIX: Use 'db.workoutDao()' instead of 'dao'
            val workout = db.workoutDao().getWorkoutById(workoutId) ?: return@launch

            // A. Setup ID & Header Info
            currentWorkoutId = workout.id
            workoutName.value = workout.name
            workoutDate.value = workout.date
            // FIX: Use 'notes' (plural) if that is what your Data Class uses
            workoutNote.value = workout.notes ?: ""
            isQuest.value = workout.isQuest

            // B. Load Exercises
            addedExercises.clear()
            addedExercises.addAll(workout.exercises)
        }
    }

    // 3. DELETE FUNCTION
    fun deleteCurrentWorkout(onDeleted: () -> Unit) {
        viewModelScope.launch {
            currentWorkoutId?.let { id ->
                val workout = db.workoutDao().getWorkoutById(id)
                if (workout != null) {
                    db.workoutDao().deleteWorkout(workout)
                    onDeleted()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveWorkout(
        improvements:List<com.dravenmiller.swolescroll.model.SkillImprovement> = emptyList(),
        onSaved: () -> Unit
    ) {
        if (addedExercises.isEmpty()) return

        // 🧠 SMART VALIDATION: Check fields based on Exercise Type
        // This ensures "Wall Sit" (Time-based) doesn't get deleted just because Weight is 0.
        val validExercises = addedExercises.mapNotNull { workoutExercise ->
            val validSets = workoutExercise.sets.filter { set ->
                val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                when (type) {
                    // 🧱 ISOMETRIC: Valid if it has Time OR Weight
                    ExerciseType.ISOMETRIC -> {
                        (set.time != null && set.time > 0) || set.weight > 0
                    }

                    // 🏃 CARDIO: Valid if it has Distance OR Time
                    ExerciseType.CARDIO -> {
                        (set.distance != null && set.distance > 0.0) || (set.time != null && set.time > 0)
                    }

                    // 🏋️ CARRY: Valid if it has Distance OR Weight
                    ExerciseType.LoadedCarry -> {
                        (set.distance != null && set.distance > 0.0) || set.weight > 0
                    }

                    // 💪 STRENGTH / DEFAULT: Needs Weight OR Reps
                    else -> {
                        set.weight > 0 || set.reps > 0
                    }
                }
            }

            // Only keep the exercise if it has at least one valid set
            if (validSets.isNotEmpty()) {
                workoutExercise.copy(sets = validSets)
            } else {
                null
            }
        }

        if (validExercises.isEmpty()) {
            android.widget.Toast.makeText(
                application, "Add some valid sets first!", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModelScope.launch {
            // 1. Determine ID (Reuse if editing, New if creating)
            val finalId = currentWorkoutId ?: java.util.UUID.randomUUID().toString()

            // 2. Create Name
            val finalName = when {
                workoutName.value.isNotBlank() -> workoutName.value
                addedExercises.isNotEmpty() -> {
                    // 👇 Make sure the database saves the broad name too!
                    val rawMuscle = addedExercises.first().exercise.muscleGroup
                    val broadMuscle = getBroadMuscleGroup(rawMuscle)
                    "$broadMuscle Day"
                }
                else -> "Untitled Workout"
            }


            // 3. Build Object
            val workout = Workout(
                id = finalId,
                name = finalName,
                date = workoutDate.value,
                exercises = validExercises, // 👈 Use the filtered list
                notes = workoutNote.value,
                isQuest = isQuest.value,
                improvements = improvements
            )

            // 4. Save to DB
            db.workoutDao().insertWorkout(workout)

            // 5. Update Exercises Table (Safety Check)
            validExercises.forEach { workoutExercise ->
                val safeExercise = if (workoutExercise.exercise.type == null) {
                    workoutExercise.exercise.copy(type = ExerciseType.STRENGTH)
                } else {
                    workoutExercise.exercise
                }
                db.exerciseDao().insertExercise(safeExercise)
            }

            // 6. Backup & Cleanup
            val allWorkouts = db.workoutDao().getAllWorkouts().first()
            val allExercises = db.exerciseDao().getAllExercises().first()
            BackupManager.saveDataToStorage(application, allWorkouts, allExercises)

            android.widget.Toast.makeText(application, "Saved!", android.widget.Toast.LENGTH_SHORT)
                .show()

            db.draftDao().clearDraft()

            onSaved()
        }
    }


    fun splitCardioSet(
        exerciseId: String,
        currentSetIndex: Int,
        elapsedSeconds: Int,
        newIncline: Double,
        newLevel: Int
    ) {
        val index = addedExercises.indexOfFirst { it.id == exerciseId }
        if (index == -1) return

        val currentExercise = addedExercises[index]
        val currentSets = currentExercise.sets.toMutableList()

        // 1. CALCULATE DURATION OF THE PREVIOUS INTERVAL
        val previousTime =
            currentSets.filterIndexed { idx, _ -> idx != currentSetIndex }.sumOf { it.time ?: 0 }
        val thisSetDuration = elapsedSeconds - previousTime

        // 🧠 10-SECOND NOISE FILTER
        // If the previous interval lasted less than 10 seconds, we assume the user was just
        // scrolling/tapping through levels and hasn't "settled" yet.
        // We MERGE it into the new setting instead of creating a tiny 1-second set.
        if (thisSetDuration < 10 && currentSets.isNotEmpty()) { // 👈 CHANGED FROM 1 TO 10
            val lastIndex = currentSets.lastIndex
            val lastSet = currentSets[lastIndex]

            // Overwrite the previous set's settings with the NEW settings
            // This effectively "erases" the short interval
            currentSets[lastIndex] = lastSet.copy(
                weight = newIncline,
                reps = newLevel
            )
        } else {
            // 💾 LOCK & SPLIT (It was longer than 10s, so it's a valid interval)

            // A. Finalize the duration of the old set
            if (currentSetIndex in currentSets.indices) {
                val oldSet = currentSets[currentSetIndex]
                currentSets[currentSetIndex] = oldSet.copy(time = thisSetDuration)
            }

            // B. Create the NEW set
            val newSet = Set(
                id = java.util.UUID.randomUUID().toString(),
                weight = newIncline,
                reps = newLevel,
                distance = 0.0,
                time = 0
            )
            currentSets.add(newSet)
        }

        // Save changes
        addedExercises[index] = currentExercise.copy(sets = currentSets)
    }


    fun applyTreadmillDistance(totalDistance: Double) {
        // Loop through all added exercises
        addedExercises.forEachIndexed { index, workoutExercise ->
            val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH
            if (type == ExerciseType.TREADMILL) {

                // Run the magic math helper
                val updatedSets = distributeTotalDistance(totalDistance, workoutExercise.sets)

                // Update the list with the calculated distances
                addedExercises[index] = workoutExercise.copy(sets = updatedSets)
            }
        }
    }

    /**
     * Distributes a TOTAL distance across multiple sets based on their Duration and Level (Speed).
     * * Logic: Time * Level = Effort.
     * A set with higher Level gets a larger chunk of the total distance.
     */
    fun distributeTotalDistance(
        totalDistance: Double,
        sets: List<Set>
    ): List<Set> {
        // 1. Calculate "Effort Points" for each set
        // We use (Level * Seconds) to weight it.
        // If Level is 0 (e.g. cooldown), we treat it as 1 so it still gets *some* distance.
        val setEfforts = sets.map { set ->
            val level = if (set.reps > 0) set.reps / 10.0 else 1.0 // Remember Level is Reps/10
            val seconds = set.time ?: 0

            // The "Score" for this set
            val effortScore = level * seconds

            Pair(set, effortScore)
        }

        // 2. Sum up Total Effort
        val totalEffort = setEfforts.sumOf { it.second }

        // Avoid divide-by-zero if something weird happens
        if (totalEffort == 0.0) return sets

        // 3. Distribute Distance based on Percentage of Effort
        return setEfforts.map { (set, score) ->
            val percentage = score / totalEffort
            val assignedDistance = totalDistance * percentage

            // Round to 2 decimal places for cleanliness
            val cleanDistance = String.format("%.2f", assignedDistance).toDouble()

            set.copy(distance = cleanDistance)
        }
    }

    fun applyDistanceToExercise(exerciseId: String, distance: Double) {
        val index = addedExercises.indexOfFirst { it.id == exerciseId }
        if (index == -1) return
        val activeExercise = addedExercises[index]
        val updatedSets = distributeTotalDistance(distance, activeExercise.sets)
        addedExercises[index] = activeExercise.copy(sets = updatedSets)
    }
    // In LogWorkoutViewModel.kt

    // ⚡ SUPERSET HELPER: Checks if we need a new set before switching
    fun prepareForSuperset(targetIndex: Int) {
        // 1. Safety Checks
        if (targetIndex < 0 || targetIndex >= addedExercises.size) return

        val targetExercise = addedExercises[targetIndex]
        val lastSet = targetExercise.sets.lastOrNull()

        // 2. Decide if we need a new line
        val needsNewSet = if (lastSet == null) {
            true // No sets at all? Definitely add one.
        } else {
            // Check if the last set is "In Use"
            val type = targetExercise.exercise.type ?: ExerciseType.STRENGTH
            val isUsed = when (type) {
                ExerciseType.CARDIO -> (lastSet.distance ?: 0.0) > 0 || (lastSet.time ?: 0) > 0
                // For lifting: If you typed Weight OR Reps, we assume that line is taken.
                else -> (lastSet.weight > 0.0) || (lastSet.reps > 0)
            }
            isUsed
        }

        // 3. Add the set if needed
        if (needsNewSet) {
            val newSet = Set(
                id = java.util.UUID.randomUUID().toString(),
                weight = 0.0,
                reps = 0,
                distance = 0.0,
                time = 0
            )
            // Update the list
            val updatedSets = targetExercise.sets + newSet
            addedExercises[targetIndex] = targetExercise.copy(sets = updatedSets)
        }
    }

    fun renameExercise(oldName: String, newName: String) {
        viewModelScope.launch {
            // JOB 1: Fix the Master List
            // USE db.exerciseDao(), not dao
            db.exerciseDao().renameExercise(oldName, newName)

            // JOB 2: Fix the History
            // USE db.workoutDao(), not workoutDao
            val allWorkouts = db.workoutDao().getAllWorkoutsList()

            allWorkouts.forEach { workout ->
                val hasOldName = workout.exercises.any { it.exercise.name == oldName }

                if (hasOldName) {
                    val updatedExercises = workout.exercises.map { workoutExercise ->
                        if (workoutExercise.exercise.name == oldName) {
                            val fixedExercise = workoutExercise.exercise.copy(
                                name = newName,
                                type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH
                            )
                            workoutExercise.copy(exercise = fixedExercise)
                        } else {
                            workoutExercise
                        }
                    }

                    // Save back to DB
                    db.workoutDao().updateWorkout(workout.copy(exercises = updatedExercises))
                }
            }

            // JOB 3: Refresh the UI
            // REMOVED: _exerciseList.value = ...
            // REASON: Your 'exerciseList' variable at the top is a Flow.
            // It watches the database and will update itself automatically!
        }
    }


}

// Update Factory to pass Application and DB
class LogWorkoutViewModelFactory(
    private val application: Application,
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogWorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogWorkoutViewModel(application, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

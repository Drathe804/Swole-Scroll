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
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.map
import kotlin.collections.sumOf

// Change "ViewModel" to "AndroidViewModel(application)"
class LogWorkoutViewModel(
    private val application: Application, // Needs this to save files
    private val db: AppDatabase // Pass the whole DB for easier access
) : AndroidViewModel(application) {



    // ... (Keep your existing State variables: name, date, note, lists) ...
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
    var currentWorkoutId: String? = null

    val exerciseList = db.exerciseDao().getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // Inside LogWorkoutViewModel.kt

    // 1. SMART PR TRACKER (Totals + Dominant Level) 🏆
    val personalRecords: kotlinx.coroutines.flow.StateFlow<Map<String, String>> =
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
                                // 🏃 CARDIO: Calculate SPEED (Distance / Time)
                                // We sum the session to get average speed
                                val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }

                                if (totalDist > 0 && totalSeconds > 0) {
                                    val speed = totalDist / (totalSeconds / 3600.0) // Miles per Hour

                                    val currentBest = maxCardioSpeed[name] ?: 0.0
                                    if (speed > currentBest) {
                                        maxCardioSpeed[name] = speed
                                        // Save as "5.2 mph"
                                        val niceSpeed = String.format("%.2f", speed)
                                        prMap[name] = "$niceSpeed mph"
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


    init {
        checkForDraft()
    }
    val exerciseNotesHistory: kotlinx.coroutines.flow.StateFlow<Map<String, List<String>>> =
        db.workoutDao().getAllWorkouts()
            .map { workouts ->
                val notesMap = mutableMapOf<String, MutableList<String>>()
                val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())

                workouts.forEach { workout ->
                    val dateStr = dateFormat.format(java.util.Date(workout.date))
                    workout.exercises.forEach { workoutExercise ->
                        if (!workoutExercise.note.isNullOrBlank()){
                            val entry = "$dateStr: ${workoutExercise.note}"
                            notesMap.getOrPut(workoutExercise.exercise.name) { mutableListOf() }.add(entry)
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

    fun loadHistory(targetExerciseId: String, name: String) {
        historyTitle = name
        viewModelScope.launch {
            val allWorkouts = db.workoutDao().getAllWorkouts().first()
            val foundHistory = allWorkouts.flatMap { workout -> workout.exercises }
                //.filter { it.exercise.id == targetExerciseId }
                .filter { it.exercise.name.trim().equals(name.trim(), ignoreCase = true) }
                _exerciseHistory.value = foundHistory
        }
    }

    private fun checkForDraft() {
        viewModelScope.launch {
            val draft = db.draftDao().getDraft()
            if (draft != null) {
                val savedWorkout = Gson().fromJson(draft.dataJson, Workout::class.java)
                pendingDraft = savedWorkout
                showResumeDialog.value = true
            }
        }
    }
    fun resumeDraft(){
        pendingDraft?.let { workout ->
            workoutName.value = workout.name
            workoutDate.value = workout.date
            workoutNote.value = workout.notes
            addedExercises.clear()
            addedExercises.addAll(workout.exercises)
        }
        showResumeDialog.value = false
    }

    fun discardDraft(){
        viewModelScope.launch {
            db.draftDao().clearDraft()
        }
        showResumeDialog.value = false
    }

    fun autoSaveDraft(){
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
        type: ExerciseType
    ){
        viewModelScope.launch {
            val cleanName = name.trim()
            val existingExercise = db.exerciseDao().getExerciseByName(cleanName)
            val exerciseToUse = if (existingExercise != null) {
                if (existingExercise.type != null){
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
                    type = type
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
            // 1. Find the ORIGINAL version of this exercise to see what the old name was
            val existingExercise = exerciseList.value.find { it.id == updatedExercise.id }

            if (existingExercise != null && existingExercise.name != updatedExercise.name) {
                // 🚨 NAME CHANGED! 🚨
                // Trigger the History Migrator to fix past logs
                renameExercise(oldName = existingExercise.name, newName = updatedExercise.name)

                // Also ensure muscle/type updates happen
                db.exerciseDao().updateExercise(updatedExercise)
            } else {
                // 💤 NAME IS THE SAME
                // Just update the muscle group or type in the master list
                db.exerciseDao().updateExercise(updatedExercise)
            }
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
    fun saveWorkout(onSaved: () -> Unit) {
        if (addedExercises.isEmpty()) return

        // Simple Validation
        val validExercises = addedExercises.filter { exercise ->
            exercise.sets.any { set ->
                (set.weight > 0.0) || (set.reps > 0) || ((set.distance ?: 0.0) > 0)
            }
        }
        if (validExercises.isEmpty()) {
            android.widget.Toast.makeText(
                application, "Add some sets first!", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModelScope.launch {
            // 1. Determine ID (Reuse if editing, New if creating)
            val finalId = currentWorkoutId ?: java.util.UUID.randomUUID().toString()

            // 2. Create Name
            val finalName = when {
                workoutName.value.isNotBlank() -> workoutName.value
                addedExercises.isNotEmpty() -> "${addedExercises.first().exercise.muscleGroup} Day"
                else -> "Untitled Workout"
            }

            // 3. Build Object
            val workout = Workout(
                id = finalId,
                name = finalName,
                date = workoutDate.value,
                exercises = validExercises,
                notes = workoutNote.value
            )

            // 4. Save to DB
            db.workoutDao().insertWorkout(workout)

            // 5. Update Exercises Table (🛡️ WITH CRASH FIX)
            validExercises.forEach { workoutExercise ->
                // CHECK: Is the Type null? If so, force it to STRENGTH.
                val safeExercise = if (workoutExercise.exercise.type == null) {
                    workoutExercise.exercise.copy(type = ExerciseType.STRENGTH)
                } else {
                    workoutExercise.exercise
                }
                // Save the safe version
                db.exerciseDao().insertExercise(safeExercise)
            }

            // 6. Backup & Cleanup
            val allWorkouts = db.workoutDao().getAllWorkouts().first()
            val allExercises = db.exerciseDao().getAllExercises().first()
            BackupManager.saveDataToStorage(application, allWorkouts, allExercises)

            android.widget.Toast.makeText(application, "Saved!", android.widget.Toast.LENGTH_SHORT).show()

            db.draftDao().clearDraft()

            onSaved()
        }
    }


    fun splitCardioSet(
        exerciseId: String,
        currentSetIndex: Int,
        elapsedSeconds: Int, // This is the TOTAL workout time now
        newIncline: Double,
        newLevel: Int
    ) {
        val index = addedExercises.indexOfFirst { it.id == exerciseId }
        if (index == -1) return

        val currentExercise = addedExercises[index]
        val currentSets = currentExercise.sets.toMutableList()

        // 1. CALCULATE MATH: Subtract time from previous sets to get THIS set's duration
        // Sum up all sets EXCEPT the current one we are editing
        val previousTime = currentSets.filterIndexed { idx, _ -> idx != currentSetIndex }.sumOf { it.time }

        // This set's actual length = Total Timer - Time used in previous sets
        val thisSetDuration = elapsedSeconds - previousTime

        // 🧠 SMART CHECK: Rapid Taps (using the calculated duration)
        // If this specific level lasted less than 1 second, just update, don't split.
        if (thisSetDuration < 1 && currentSets.isNotEmpty()) {
            val lastIndex = currentSets.lastIndex
            val lastSet = currentSets[lastIndex]

            // Overwrite settings (Rapid fire correction)
            currentSets[lastIndex] = lastSet.copy(
                weight = newIncline,
                reps = newLevel
            )
        }
        else {
            // 💾 NORMAL LOGIC: Lock & Split

            // A. Update the OLD set with its calculated duration
            if (currentSetIndex in currentSets.indices) {
                val oldSet = currentSets[currentSetIndex]
                currentSets[currentSetIndex] = oldSet.copy(time = thisSetDuration)
            }

            // B. Create the NEW set
            // Note: We don't save 'elapsedSeconds' here. The time is 0 until we calculate it later.
            val newSet = com.dravenmiller.swolescroll.model.Set(
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
            if (workoutExercise.exercise.name.contains("Treadmill", ignoreCase = true)) {

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
        sets: List<com.dravenmiller.swolescroll.model.Set>
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

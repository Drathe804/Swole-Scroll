package com.example.swolescroll.features.logworkout

import android.app.Application // Import this!
import android.util.Log
import android.util.Log.e
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel // Import this!
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swolescroll.data.AppDatabase
import com.example.swolescroll.data.BackupManager // Import your new manager
import com.example.swolescroll.model.Draft
import com.example.swolescroll.model.Exercise
import com.example.swolescroll.model.ExerciseType
import com.example.swolescroll.model.Set
import com.example.swolescroll.model.Workout
import com.example.swolescroll.model.WorkoutExercise
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

    val exerciseList = db.exerciseDao().getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1. SMART PR TRACKER (With Incline, Distance & Crash Protection) 🏆
    val personalRecords: kotlinx.coroutines.flow.StateFlow<Map<String, String>> =
        db.workoutDao().getAllWorkouts()
            .map { workouts ->
                val prMap = mutableMapOf<String, String>()
                val maxValues = mutableMapOf<String, Double>()

                workouts.forEach { workout ->
                    workout.exercises.forEach { workoutExercise ->
                        val name = workoutExercise.exercise.name

                        // 🛡️ CRASH FIX: Default to STRENGTH if type is null
                        val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                        // A. Find Best Set
                        val bestSet = when (type) {
                            ExerciseType.CARDIO -> workoutExercise.sets.maxByOrNull { it.distance ?: 0.0 }
                            else -> workoutExercise.sets.maxByOrNull { it.weight }
                        }

                        if (bestSet != null) {
                            // B. Get Value to Compare
                            val newValue = when (type) {
                                ExerciseType.CARDIO -> bestSet.distance ?: 0.0
                                else -> bestSet.weight
                            }

                            val currentMax = maxValues[name] ?: 0.0

                            if (newValue > currentMax) {
                                maxValues[name] = newValue

                                // C. Format String
                                prMap[name] = when (type) {
                                    ExerciseType.CARDIO -> {
                                        val isStairs = name.contains("Stair", true) || name.contains("Step", true)
                                        val unit = if (isStairs) "stairs" else "mi"
                                        val timeStr = bestSet.timeFormatted()

                                        // 🏃‍♂️ INTENSITY LOGIC
                                        val intensityStr = if (name.contains("Treadmill", true)) {
                                            // Treadmill: Speed (Reps/10) AND Incline (Weight)
                                            val speed = bestSet.reps / 10.0
                                            val incline = bestSet.weight

                                            // Build the string dynamically so it looks clean
                                            val details = mutableListOf<String>()
                                            if (speed > 0) details.add("Lvl $speed")
                                            if (incline > 0) details.add("$incline% Inc")

                                            if (details.isNotEmpty()) " (@ ${details.joinToString(", ")})" else ""
                                        } else {
                                            // Bike/Stairs: Resistance Level (Weight)
                                            if (bestSet.weight > 0) " (@ Lvl ${bestSet.weight})" else ""
                                        }

                                        "$newValue $unit in $timeStr$intensityStr"
                                    }

                                    // 🏋️‍♂️ CARRIES: Add Distance!
                                    ExerciseType.LoadedCarry -> "$newValue lbs for ${bestSet.distance ?: 0.0} yds"

                                    ExerciseType.ISOMETRIC -> "$newValue lbs"
                                    else -> "$newValue lbs x ${bestSet.reps}"
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
                existingExercise
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
            val newWorkoutExercise = WorkoutExercise(
                exercise = exerciseToUse,
                sets = emptyList()
            )
            addedExercises.add(newWorkoutExercise)
        }
    }

    fun updateExercise(updatedExercise: Exercise) {
        viewModelScope.launch {
            db.exerciseDao().updateExercise(updatedExercise)
        }
    }

    fun saveWorkout(onSaved: () -> Unit) {
        if (addedExercises.isEmpty()) return
        val validExercises = addedExercises.filter { it.sets.isNotEmpty() }
        if (validExercises.isEmpty()) {
            android.widget.Toast.makeText(
                application, "Add some sets first!", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModelScope.launch {

            val finalName = when {
                workoutName.value.isNotBlank() -> workoutName.value

                addedExercises.isNotEmpty() -> "${addedExercises.first().exercise.muscleGroup} Day"
                else -> "Untitled Workout"
            }
            // 1. Save to DB
            val workout = Workout(
                name = finalName,
                date = workoutDate.value,
                exercises = validExercises,
                notes = workoutNote.value
            )
            db.workoutDao().insertWorkout(workout)

            addedExercises.forEach { workoutExercise ->
                db.exerciseDao().insertExercise(workoutExercise.exercise)
            }

            // 2. AUTOMATIC BACKUP TRIGGER
            // Grab the latest data from DB
            val allWorkouts = db.workoutDao().getAllWorkouts().first()
            val allExercises = db.exerciseDao().getAllExercises().first()

            // Write to file
            BackupManager.saveDataToStorage(application, allWorkouts, allExercises)
            android.widget.Toast.makeText(
                application,
                "Workout Saved & Backed Up!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
            val newSet = com.example.swolescroll.model.Set(
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
        sets: List<com.example.swolescroll.model.Set>
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

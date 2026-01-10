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
    // Inside LogWorkoutViewModel.kt

    // 1. SMART PR TRACKER (Totals + Dominant Level) 🏆
    val personalRecords: kotlinx.coroutines.flow.StateFlow<Map<String, String>> =
        db.workoutDao().getAllWorkouts()
            .map { workouts ->
                val prMap = mutableMapOf<String, String>()
                val maxValues = mutableMapOf<String, Double>()

                workouts.forEach { workout ->
                    workout.exercises.forEach { workoutExercise ->
                        val name = workoutExercise.exercise.name
                        val type = workoutExercise.exercise.type ?: ExerciseType.STRENGTH

                        // A. CALCULATE THE VALUE TO COMPARE
                        val (candidateValue, displayString) = when (type) {
                            ExerciseType.CARDIO -> {
                                // 1. TOTALS: Sum up the entire session
                                val totalDist = workoutExercise.sets.sumOf { it.distance ?: 0.0 }
                                val totalSeconds = workoutExercise.sets.sumOf { it.time ?: 0 }

                                // 2. MAJORITY LEVEL: Find the level used for the most TIME ⏳
                                // Group sets by Level (reps) -> Sum their duration -> Pick the winner
                                val dominantLevelEntry = workoutExercise.sets
                                    .groupBy { it.reps } // Group by Level
                                    .maxByOrNull { entry -> entry.value.sumOf { it.time ?: 0 } } // Find max duration

                                val majorityLevelRaw = dominantLevelEntry?.key ?: 0

                                // 3. FORMATTING
                                val isStairs = name.contains("Stair", true) || name.contains("Step", true)
                                val unit = if (isStairs) "stairs" else "mi"

                                // Helper to format time (e.g. 3600s -> "60:00")
                                val h = totalSeconds / 3600
                                val m = (totalSeconds % 3600) / 60
                                val s = totalSeconds % 60
                                val timeFormatted = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)

                                // Intensity String (based on the MAJORITY level)
                                val intensityStr = if (name.contains("Treadmill", true)) {
                                    val speed = majorityLevelRaw / 10.0
                                    // Note: We aren't calculating majority incline here to keep it simple, but you could!
                                    if (speed > 0) " (@ Lvl $speed)" else ""
                                } else {
                                    // Bike/Stairs (Level is in 'weight')
                                    // We need to do the same "Majority" logic for weight if it's a Bike
                                    // For simplicity, let's grab the weight from the dominant set group
                                    val dominantWeight = dominantLevelEntry?.value?.firstOrNull()?.weight ?: 0.0
                                    if (dominantWeight > 0) " (@ Lvl $dominantWeight)" else ""
                                }

                                // RETURN: Pair(The Distance Number, The Nice String)
                                Pair(totalDist, "$totalDist $unit in $timeFormatted$intensityStr")
                            }

                            // NON-CARDIO: Standard Max Logic
                            else -> {
                                val bestSet = workoutExercise.sets.maxByOrNull { it.weight }
                                val bestVal = bestSet?.weight ?: 0.0
                                val str = if (type == ExerciseType.LoadedCarry) {
                                    "$bestVal lbs for ${bestSet?.distance ?: 0.0} yds"
                                } else if (type == ExerciseType.ISOMETRIC) {
                                    "$bestVal lbs"
                                } else {
                                    "$bestVal lbs x ${bestSet?.reps}"
                                }
                                Pair(bestVal, str)
                            }
                        }

                        // B. COMPARE & SAVE
                        // Only update if this session was "better" (More Distance or More Weight)
                        val currentMax = maxValues[name] ?: 0.0
                        if (candidateValue > currentMax) {
                            maxValues[name] = candidateValue
                            prMap[name] = displayString
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

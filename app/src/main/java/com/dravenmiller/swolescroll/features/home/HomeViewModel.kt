package com.dravenmiller.swolescroll.features.home

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.data.BackupManager
import com.dravenmiller.swolescroll.features.quests.QuestDifficulty
import com.dravenmiller.swolescroll.features.quests.QuestManager
import com.dravenmiller.swolescroll.model.Draft
import com.dravenmiller.swolescroll.model.Workout
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

// IMPORTANT: Inherit from AndroidViewModel (not just ViewModel)
class HomeViewModel(
    private val application: Application,
    private val db: AppDatabase
) : AndroidViewModel(application) {

    val workouts = db.workoutDao().getAllWorkouts()

    // --- THIS IS THE MISSING FUNCTION ---
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            // Call the Manager to do the heavy lifting
            BackupManager.importFromUri(application, uri, db)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun backupNow(){
        viewModelScope.launch {
            val allWorkouts = db.workoutDao().getAllWorkouts().first()
            val allExercises = db.exerciseDao().getAllExercises().first()
            BackupManager.saveDataToStorage(application, allWorkouts, allExercises)
            android.widget.Toast.makeText(
                application,
                "Backup saved to Downloads!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    val showQuestDialog = mutableStateOf(false)
    private val questManager = QuestManager(db)

    // This event tells the UI "We are ready to go to the Log Screen"
    val navigateToLog = mutableStateOf(false)

    fun acceptQuest(difficulty: QuestDifficulty) {
        viewModelScope.launch {
            showQuestDialog.value = false // Close dialog

            // 1. Generate the Exercises
            val result = questManager.generateQuest(difficulty)

            // 2. Create a "Ghost" Workout
            val questWorkout = Workout(
                id = UUID.randomUUID().toString(),
                name = result.title,
                date = System.currentTimeMillis(),
                exercises = result.exercises,
                notes = result.notes,
                isQuest = true
            )

            // 3. Save as Draft so LogWorkoutScreen picks it up
            val json = Gson().toJson(questWorkout)
            db.draftDao().insertDraft(Draft(dataJson = json))

            // 4. Trigger Navigation
            navigateToLog.value = true
        }
    }

    // Reset navigation flag after handling
    fun onNavigationHandled() {
        navigateToLog.value = false
    }
}

// The Factory needs to pass the Application and DB
class HomeViewModelFactory(
    private val application: Application,
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

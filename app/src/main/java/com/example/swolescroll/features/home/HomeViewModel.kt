package com.example.swolescroll.features.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swolescroll.data.AppDatabase
import com.example.swolescroll.data.BackupManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

package com.dravenmiller.swolescroll.features.detail

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.data.WorkoutDao
import com.dravenmiller.swolescroll.model.Workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val dao: WorkoutDao,
    private val workoutId: String,
    private val db: AppDatabase
) : ViewModel() {

    // State: The workout we are looking at (starts as null until we load it)
    val workout = mutableStateOf<Workout?>(null)
    val userWeight = db.userDao().getUserProfile()
        .map { it?.bodyWeight ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // When this ViewModel starts, load the data!
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            workout.value = dao.getWorkoutById(workoutId)
        }
    }

    fun deleteWorkout(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workout.value?.let { currentWorkout ->
                dao.deleteWorkout(currentWorkout)
                onDeleted()
            }
        }
    }

    fun updateWorkoutName(newName: String) {
        val currentWorkout = workout.value?: return
        viewModelScope.launch {
            val updatedWorkout = currentWorkout.copy(name = newName)
            dao.updateWorkout(updatedWorkout)
            workout.value = updatedWorkout
        }
    }
}

// Factory to help Android build this (since it needs an ID)
class WorkoutDetailViewModelFactory(
    private val dao: WorkoutDao,
    private val workoutId: String,
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutDetailViewModel(dao, workoutId, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

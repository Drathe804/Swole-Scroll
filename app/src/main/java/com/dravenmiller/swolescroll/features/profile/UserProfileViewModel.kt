package com.dravenmiller.swolescroll.features.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()

    // UI State
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _showMonthlyPrompt = MutableStateFlow(false)
    val showMonthlyPrompt = _showMonthlyPrompt.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userDao.getUserProfile().collect { profile ->
                _userProfile.value = profile

                // CHECK MONTHLY PROMPT 📅
                if (profile != null) {
                    val thirtyDays = 30L * 24 * 60 * 60 * 1000 // Milliseconds
                    val timeSinceUpdate = System.currentTimeMillis() - profile.lastWeightUpdate

                    if (timeSinceUpdate > thirtyDays) {
                        _showMonthlyPrompt.value = true
                    }
                }
            }
        }
    }

    fun saveProfile(name: String, weight: Double, difficulty: String) {
        viewModelScope.launch {
            // Keep existing ID/Timestamp if updating
            val current = _userProfile.value
            val updated = current?.copy(
                name = name,
                bodyWeight = weight,
                defaultDifficulty = difficulty,
                // Update timestamp ONLY if weight changed (or force update it?)
                // Let's force update it to reset the 30-day timer
                lastWeightUpdate = System.currentTimeMillis()
            ) ?: UserProfile(
                name = name,
                bodyWeight = weight,
                defaultDifficulty = difficulty
            )

            userDao.insertOrUpdate(updated)
            _showMonthlyPrompt.value = false // Dismiss prompt if it was open
        }
    }

    fun dismissPrompt() {
        _showMonthlyPrompt.value = false
    }
}

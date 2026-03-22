package com.dravenmiller.swolescroll.features.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dravenmiller.swolescroll.data.AppDatabase
import com.dravenmiller.swolescroll.model.ExerciseType
import com.dravenmiller.swolescroll.model.UserProfile
import com.dravenmiller.swolescroll.model.Workout
import com.dravenmiller.swolescroll.util.BodyweightMath
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val draftDao = db.draftDao() // 👈 Added your DraftDao!

    // UI State
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _showMonthlyPrompt = MutableStateFlow(false)
    val showMonthlyPrompt = _showMonthlyPrompt.asStateFlow()

    // 🎚️ SIEGE MODE TOGGLE STATE
    private val prefs = application.getSharedPreferences("SwoleScrollPrefs", android.content.Context.MODE_PRIVATE)
    private val _isSiegeModeEnabled = MutableStateFlow(prefs.getBoolean("siege_mode", true))
    val isSiegeModeEnabled = _isSiegeModeEnabled.asStateFlow()

    // 🔮 UNCLAIMED XP (DRAFT WORKOUT VOLUME)
    private val _draftVolume = MutableStateFlow(0)
    val draftVolume = _draftVolume.asStateFlow()

    init {
        loadProfile()
    }

    // 🎚️ UPDATE SIEGE MODE
    fun toggleSiegeMode(isEnabled: Boolean) {
        prefs.edit().putBoolean("siege_mode", isEnabled).apply()
        _isSiegeModeEnabled.value = isEnabled
    }

    // ⚔️ LIFETIME XP (TOTAL VOLUME)
    val lifetimeVolume = db.workoutDao().getAllWorkouts()
        .combine(userDao.getUserProfile()) { workouts, profile ->
            val uWeight = profile?.bodyWeight ?: 0.0
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


    private fun loadProfile() {
        viewModelScope.launch {
            userDao.getUserProfile().collect { profile ->
                _userProfile.value = profile

                // 🔮 Calculate Unclaimed Draft XP using body weight!
                calculateDraftVolume(profile?.bodyWeight ?: 0.0)

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

    // 🧠 GSON UNPACKER: Safely grabs the draft and calculates the Rainbow XP!
    private suspend fun calculateDraftVolume(uWeight: Double) {
        val draft = draftDao.getDraft()
        if (draft == null || draft.dataJson.isBlank()) {
            _draftVolume.value = 0
            return
        }

        try {
            val workout = Gson().fromJson(draft.dataJson, Workout::class.java)
            var draftTotal = 0

            workout.exercises.forEach { we ->
                val type = we.exercise.type ?: ExerciseType.STRENGTH
                val multiplier = if (we.exercise.isSingleSide) 2 else 1
                val bwPercentage = BodyweightMath.getMultiplier(we.exercise.name)

                we.sets.forEach { set ->
                    val w = if (we.exercise.isBodyweight) (uWeight * bwPercentage) + set.weight else set.weight
                    val d = set.distance ?: 0.0
                    val t = set.time ?: 0

                    draftTotal += when (type) {
                        ExerciseType.STRENGTH -> (w * set.reps * multiplier).toInt()
                        ExerciseType.ISOMETRIC -> (w * t * multiplier).toInt()
                        ExerciseType.LoadedCarry -> (w * d * multiplier).toInt()
                        ExerciseType.TWENTY_ONES -> (((w * set.reps * multiplier) * 2) / 3).toInt()
                        else -> 0
                    }
                }
            }
            _draftVolume.value = draftTotal
        } catch (e: Exception) {
            _draftVolume.value = 0 // Failsafe just in case the JSON gets corrupted
        }
    }

    fun saveProfile(name: String, weight: Double, difficulty: String) {
        viewModelScope.launch {
            val current = _userProfile.value
            val updated = current?.copy(
                name = name,
                bodyWeight = weight,
                defaultDifficulty = difficulty,
                lastWeightUpdate = System.currentTimeMillis()
            ) ?: UserProfile(
                name = name,
                bodyWeight = weight,
                defaultDifficulty = difficulty
            )

            userDao.insertOrUpdate(updated)
            _showMonthlyPrompt.value = false
        }
    }

    fun dismissPrompt() {
        _showMonthlyPrompt.value = false
    }
}

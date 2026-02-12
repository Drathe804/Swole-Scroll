package com.dravenmiller.swolescroll.data

import com.dravenmiller.swolescroll.model.UserProfile
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()

    suspend fun updateProfile(name: String, weight: Double, difficulty: String) {
        val current = userDao.getUserProfileOneShot()
        val updated = current?.copy(
            name = name,
            bodyWeight = weight,
            defaultDifficulty = difficulty,
            lastWeightUpdate = System.currentTimeMillis() // Update timestamp
        ) ?: UserProfile(name = name, bodyWeight = weight, defaultDifficulty = difficulty)

        userDao.insertOrUpdate(updated)
    }

    // Call this specifically when updating JUST weight (e.g., monthly prompt)
    suspend fun updateWeightOnly(weight: Double) {
        val current = userDao.getUserProfileOneShot() ?: UserProfile()
        val updated = current.copy(
            bodyWeight = weight,
            lastWeightUpdate = System.currentTimeMillis()
        )
        userDao.insertOrUpdate(updated)
    }
}

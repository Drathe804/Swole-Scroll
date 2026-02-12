package com.dravenmiller.swolescroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile_table")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1, // Always 1 for single-user apps

    val name: String = "Hero", // Default name
    val bodyWeight: Double = 0.0,
    val defaultDifficulty: String = "RAID", // Store Enum as String (SCOUT, RAID, BOSS)
    val lastWeightUpdate: Long = System.currentTimeMillis() // To track monthly prompts
)

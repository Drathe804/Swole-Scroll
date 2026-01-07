package com.example.swolescroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draft_table")
data class Draft (
    @PrimaryKey val id: Int = 0,
    val dataJson: String
)
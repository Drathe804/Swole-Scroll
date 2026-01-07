package com.example.swolescroll

import android.app.Application
import com.example.swolescroll.data.AppDatabase

class SwoleScrollApplication : Application() {
    // Create the database instance when the app starts
    val database by lazy { AppDatabase.getDatabase(this) }
}

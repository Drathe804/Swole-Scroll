package com.dravenmiller.swolescroll

import android.app.Application
import com.dravenmiller.swolescroll.data.AppDatabase

class SwoleScrollApplication : Application() {
    // Create the database instance when the app starts
    val database by lazy { AppDatabase.getDatabase(this) }
}

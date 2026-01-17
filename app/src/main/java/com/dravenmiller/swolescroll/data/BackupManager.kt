package com.dravenmiller.swolescroll.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log.e
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.dravenmiller.swolescroll.model.BackupData
import com.dravenmiller.swolescroll.model.Exercise
import com.dravenmiller.swolescroll.model.Workout
import com.google.gson.Gson
import java.io.File
import java.io.InputStreamReader

object BackupManager {
    private val gson = Gson()
    private const val FILE_NAME = "swole_scroll_backup.json"

    // 1. SAVE (Automatic)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveDataToStorage(context: Context, workouts: List<Workout>, exercises: List<Exercise>) {
        try {
            val backupData = BackupData(workouts, exercises)
            val jsonString = gson.toJson(backupData)

            // ✅ 1. Create a Unique Filename (Timestamped)
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
            val filename = "swole_backup_$timeStamp.json"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")

                // 🛡️ API CHECK: Only use RELATIVE_PATH on Android 10+ (API 29)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            // 💾 Save using MediaStore (Works on all versions)
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    // 2. IMPORT (From a file you picked)
    suspend fun importFromUri(context: Context, uri: Uri, db: AppDatabase): Boolean {
        return try {
            // Read the file the user picked
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val reader = InputStreamReader(inputStream)
            val data = gson.fromJson(reader, BackupData::class.java)

            // Overwrite Database
            // (In a real app, you might want to 'merge' instead, but this is safer for restoring)
            data.exercises.forEach { db.exerciseDao().insertExercise(it) }
            data.workouts.forEach { db.workoutDao().insertWorkout(it) }

            inputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 3. GET URI (For Sharing)
    fun getBackupUri(context: Context): Uri? {
        val file = File(context.getExternalFilesDir(null), FILE_NAME)
        if (!file.exists()) return null

        // Generate the "Security Badge" URI
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}

package com.example.swolescroll.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.swolescroll.model.Draft
import com.example.swolescroll.model.Exercise
import com.example.swolescroll.model.Workout

// FIX 1: CHANGE VERSION TO 3
// (Your dad is on Version 2. He needs to go to 3 to get the new feature.)
@Database(entities = [Workout::class, Exercise::class, Draft::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // This stays the same (History)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_table ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        // FIX 2: UPDATE THE SQL TO CREATE THE *DRAFT* TABLE
        // (Your code had "exercise_table" here, which is wrong!)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `draft_table` (`id` INTEGER NOT NULL, `dataJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_table ADD COLUMN isSingleSide INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercise_table SET type = 'STRENGTH'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swole_scroll_database"
                )
                    // FIX 3: MAKE SURE BOTH ARE ADDED
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.dravenmiller.swolescroll.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dravenmiller.swolescroll.model.Draft
import com.dravenmiller.swolescroll.model.Exercise
import com.dravenmiller.swolescroll.model.UserProfile
import com.dravenmiller.swolescroll.model.Workout

@Database(
    entities = [
        Workout::class,
        Exercise::class,
        Draft::class,
        UserProfile::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun draftDao(): DraftDao
    abstract fun userDao(): UserDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_table ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

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
                // If you already have the column, this try-catch prevents a crash
                try {
                    db.execSQL("ALTER TABLE exercise_table ADD COLUMN type TEXT DEFAULT 'STRENGTH'")
                } catch (e: Exception) {
                    // Column likely already exists
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercise_table SET type = 'TREADMILL' WHERE name LIKE '%Treadmill%'")
                db.execSQL("UPDATE exercise_table SET type = 'STAIRS' WHERE name LIKE '%Stair%' OR name LIKE '%Step%'")
                db.execSQL("UPDATE exercise_table SET type = 'CARDIO' WHERE type = 'STRENGTH' AND (name LIKE '%Bike%' OR name LIKE '%Elliptical%')")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_table ADD COLUMN isQuest INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile_table` (
                        `id` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL DEFAULT 'Hero', 
                        `bodyWeight` REAL NOT NULL, 
                        `defaultDifficulty` TEXT NOT NULL DEFAULT 'RAID', 
                        `lastWeightUpdate` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite uses 0 for false, 1 for true
                db.execSQL("ALTER TABLE `exercise_table` ADD COLUMN `isBodyweight` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // We default to '[]' because that is an empty JSON array for your TypeConverter!
                db.execSQL("ALTER TABLE workout_table ADD COLUMN improvements TEXT NOT NULL DEFAULT '[]'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swole_scroll_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    // 👇 ADD THE CALLBACK HERE
                    .addCallback(object : Callback() {

                        // 1. RUNS EVERY TIME APP OPENS (Fixes old data silently)
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Thread {
                                try {
                                    // Double check treadmill/stairs logic every time just in case
                                    db.execSQL("UPDATE exercise_table SET type = 'TREADMILL' WHERE type = 'CARDIO' AND name LIKE '%Treadmill%'")
                                    db.execSQL("UPDATE exercise_table SET type = 'STAIRS' WHERE type = 'CARDIO' AND (name LIKE '%Stair%' OR name LIKE '%Step%')")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }.start()
                        }

                        // 2. RUNS ONCE ON INSTALL (Pre-populates list)
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Thread {
                                try {
                                    val database = getDatabase(context)
                                    // Only insert if database is empty
                                    if (database.exerciseDao().getAllExercisesList().isEmpty()) {
                                        database.exerciseDao().insertAll(PrepopulateData.defaultExercises)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }.start()
                        }
                        suspend fun syncDatabaseWithDefaults(db: AppDatabase) {
                            val dao = db.exerciseDao()
                            val defaultList = PrepopulateData.defaultExercises

                            defaultList.forEach { defaultExercise ->
                                // 1. Look for the exercise in the DB
                                val existingExercise = dao.getExerciseByName(defaultExercise.name)

                                if (existingExercise == null) {
                                    // ✅ CASE 1: Missing? Add it! (This is how Sled Pull appeared)
                                    dao.insertExercise(defaultExercise)
                                } else {
                                    // ✅ CASE 2: Exists but wrong? Fix it! (This fixes Overhead Press)
                                    // We check if the Muscle Group or Type doesn't match our new list.
                                    if (existingExercise.muscleGroup != defaultExercise.muscleGroup ||
                                        existingExercise.type != defaultExercise.type) {

                                        val updatedExercise = existingExercise.copy(
                                            muscleGroup = defaultExercise.muscleGroup,
                                            type = defaultExercise.type
                                        )
                                        dao.updateExercise(updatedExercise)
                                    }
                                }
                            }
                        }

                    })
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

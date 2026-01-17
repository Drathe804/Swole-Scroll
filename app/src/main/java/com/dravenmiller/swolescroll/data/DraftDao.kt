package com.dravenmiller.swolescroll.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dravenmiller.swolescroll.model.Draft

@Dao
interface DraftDao {
    // Get the single Draft (always use 0)
    @Query("SELECT * FROM draft_table WHERE id = 0")
    suspend fun getDraft(): Draft?

    // Save a draft. If ID 0 exists, overwrite it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: Draft)

    // Clear draft when workout is finished
    @Query("DELETE FROM draft_table WHERE id = 0")
    suspend fun clearDraft()

}
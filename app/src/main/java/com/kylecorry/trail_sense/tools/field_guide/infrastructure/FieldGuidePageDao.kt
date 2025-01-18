package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FieldGuidePageDao {
    @Query("SELECT * FROM field_guide_pages")
    suspend fun getAllPages(): List<FieldGuidePageEntity>

    @Query("SELECT * FROM field_guide_pages WHERE _id = :id LIMIT 1")
    suspend fun getPage(id: Long): FieldGuidePageEntity?

    @Delete
    suspend fun delete(page: FieldGuidePageEntity)

    @Upsert
    suspend fun upsert(page: FieldGuidePageEntity): Long
}
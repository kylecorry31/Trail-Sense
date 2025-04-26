package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FieldGuideSightingDao {
    @Query("SELECT * FROM field_guide_sightings WHERE _id = :id LIMIT 1")
    suspend fun getSighting(id: Long): FieldGuideSightingEntity?

    @Query("SELECT * FROM field_guide_sightings WHERE field_guide_page_id = :fieldGuidePageId")
    suspend fun getSightingsForPage(fieldGuidePageId: Long): List<FieldGuideSightingEntity>

    @Query("SELECT * FROM field_guide_sightings")
    suspend fun getAllSightings(): List<FieldGuideSightingEntity>

    @Delete
    suspend fun delete(sighting: FieldGuideSightingEntity)

    @Query("DELETE FROM field_guide_sightings WHERE field_guide_page_id = :fieldGuidePageId")
    suspend fun deleteAllSightingsForPage(fieldGuidePageId: Long)

    @Upsert
    suspend fun upsert(sighting: FieldGuideSightingEntity): Long
}
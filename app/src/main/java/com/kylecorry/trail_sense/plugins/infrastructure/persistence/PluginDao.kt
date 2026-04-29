package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PluginDao {

    @Query("SELECT * FROM plugins")
    suspend fun getAll(): List<PluginEntity>

    @Query("SELECT * FROM plugins WHERE package_id = :packageId LIMIT 1")
    suspend fun get(packageId: String): PluginEntity?

    @Upsert
    suspend fun upsert(plugin: PluginEntity)

    @Query("DELETE FROM plugins WHERE package_id = :packageId")
    suspend fun deleteByPackageId(packageId: String)
}

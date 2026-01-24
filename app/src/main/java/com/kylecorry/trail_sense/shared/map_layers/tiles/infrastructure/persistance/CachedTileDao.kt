package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CachedTileDao {

    @Query("SELECT * FROM cached_tiles WHERE `key` = :key AND x = :x AND y = :y AND z = :z LIMIT 1")
    suspend fun get(key: String, x: Int, y: Int, z: Int): CachedTileEntity?

    @Upsert
    suspend fun upsert(entity: CachedTileEntity): Long

    @Delete
    suspend fun delete(entity: CachedTileEntity)

    @Query("DELETE FROM cached_tiles WHERE created_on < :minEpochMillis")
    suspend fun deleteCreatedBefore(minEpochMillis: Long)

    @Query("DELETE FROM cached_tiles WHERE _id IN (SELECT _id FROM cached_tiles ORDER BY last_used_on ASC LIMIT :count)")
    suspend fun deleteLeastRecentlyUsed(count: Int)

    @Query("SELECT COALESCE(SUM(size_bytes), 0) FROM cached_tiles")
    suspend fun getTotalSizeBytes(): Long

    @Query("UPDATE cached_tiles SET last_used_on = :lastUsedOnMillis WHERE _id = :id")
    suspend fun updateLastUsedOn(id: Long, lastUsedOnMillis: Long)

    @Query("SELECT filename FROM cached_tiles")
    suspend fun getAllFilenames(): List<String>

    @Query("DELETE FROM cached_tiles WHERE filename IN (:filenames)")
    suspend fun deleteByFilenames(filenames: List<String>)

    @Query("SELECT filename FROM cached_tiles WHERE `key` = :key")
    suspend fun getFilenamesByKey(key: String): List<String>

    @Query("DELETE FROM cached_tiles WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM cached_tiles")
    suspend fun deleteAll()
}

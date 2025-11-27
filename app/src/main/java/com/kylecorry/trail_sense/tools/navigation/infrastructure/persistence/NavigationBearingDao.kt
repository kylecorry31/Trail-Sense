package com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationBearingDao {

    @Query("SELECT * FROM navigation_bearings WHERE is_active = 1 LIMIT 1")
    fun getActiveBearingFlow(): Flow<NavigationBearingEntity?>

    @Query("UPDATE navigation_bearings SET is_active = 0 WHERE is_active = 1")
    suspend fun clearActiveBearing()

    @Query("SELECT 1 FROM navigation_bearings WHERE is_active = 1 LIMIT 1")
    suspend fun isNavigating(): Boolean

    @Upsert
    suspend fun upsert(bearing: NavigationBearingEntity): Long

    @Query("DELETE FROM navigation_bearings WHERE start_time < :minEpochMillis AND is_active = 0")
    suspend fun deleteOlderThan(minEpochMillis: Long)

}
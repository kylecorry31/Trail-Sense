package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import java.time.Instant

@Dao
interface BatteryDao {
    @Query("SELECT * FROM battery")
    fun get(): LiveData<List<BatteryReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BatteryReadingEntity): Long

    @Query("DELETE FROM battery WHERE time < :instant")
    suspend fun deleteOlderThan(instant: Instant)
}
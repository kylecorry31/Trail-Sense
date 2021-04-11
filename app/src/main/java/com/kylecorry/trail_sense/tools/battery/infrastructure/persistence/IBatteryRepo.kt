package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import java.time.Instant

interface IBatteryRepo {
    fun get(): LiveData<List<BatteryReadingEntity>>
    suspend fun add(reading: BatteryReadingEntity)
    suspend fun deleteBefore(time: Instant)
}
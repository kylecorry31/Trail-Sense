package com.kylecorry.trail_sense.tools.battery.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import java.time.Instant

interface IBatteryRepo {
    fun get(): LiveData<List<BatteryReading>>
    suspend fun add(reading: BatteryReading)
    suspend fun deleteBefore(time: Instant)
    fun getMaxCapacity(): Float
}
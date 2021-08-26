package com.kylecorry.trail_sense.weather.infrastructure.persistence

import androidx.lifecycle.LiveData
import java.time.Instant

interface IPressureRepo {
    fun getPressures(): LiveData<List<PressureReadingEntity>>

    suspend fun getPressuresSync(): List<PressureReadingEntity>

    suspend fun getPressure(id: Long): PressureReadingEntity?

    suspend fun deletePressure(pressure: PressureReadingEntity)

    suspend fun addPressure(pressure: PressureReadingEntity)

    suspend fun deleteOlderThan(instant: Instant)
}
package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.tools.tides.domain.TideEntity

interface ITideRepo {
    fun getTides(): LiveData<List<TideEntity>>

    suspend fun getTidesSuspend(): List<TideEntity>

    suspend fun getTide(id: Long): TideEntity?

    suspend fun deleteTide(tide: TideEntity)

    suspend fun addTide(tide: TideEntity)
}
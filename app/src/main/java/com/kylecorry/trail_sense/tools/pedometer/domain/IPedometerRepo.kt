package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.StepSessionEntity
import kotlinx.coroutines.flow.Flow

interface IPedometerRepo {

    fun getSessions(): Flow<List<StepSessionEntity>>

    suspend fun addSession(session: StepSessionEntity)

    suspend fun getLastSession(): StepSessionEntity?

    suspend fun reset()

}
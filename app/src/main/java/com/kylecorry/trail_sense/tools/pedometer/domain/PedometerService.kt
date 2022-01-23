package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.StepSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant

class PedometerService(private val repo: IPedometerRepo) : IPedometerService {

    override suspend fun addSteps(steps: Int, time: Instant) {
        val lastSession = repo.getLastSession()
        if (lastSession != null && lastSession.toStepSession().time.contains(time)) {
            repo.addSession(lastSession.copy(steps = lastSession.steps + steps))
            return
        }

        val session = StepSessionEntity(0, steps, time, time.plus(Duration.ofHours(1)))
        repo.addSession(session)
    }

    override fun getSteps(): Flow<List<StepSession>> {
        return repo.getSessions().map { sessions -> sessions.map { it.toStepSession() } }
    }

    override suspend fun reset() {
        repo.reset()
    }

}
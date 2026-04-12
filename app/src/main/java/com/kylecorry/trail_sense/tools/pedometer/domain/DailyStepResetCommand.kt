package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.andromeda.core.specifications.Specification
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IStepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IsTodaySpecification
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.PedometerSessionRepo
import kotlinx.coroutines.runBlocking
import java.time.Instant

class DailyStepResetCommand(
    private val counter: IStepCounter,
    private val sessionRepo: PedometerSessionRepo? = null,
    private val paceCalculator: IPaceCalculator? = null,
    private val isToday: Specification<Instant> = IsTodaySpecification()
) : Command {
    override fun execute() {
        val wasResetToday = counter.startTime?.let(isToday::isSatisfiedBy) ?: false
        if (!wasResetToday) {
            saveSession()
            counter.reset()
        }
    }

    private fun saveSession() {
        val repo = sessionRepo ?: return
        val calculator = paceCalculator ?: return
        val steps = counter.steps
        val startTime = counter.startTime ?: return
        if (steps <= 0) return

        val distance = calculator.distance(steps).meters().value
        val session = PedometerSession(
            0,
            startTime,
            Instant.now(),
            steps,
            distance
        )
        // Runs synchronously since this executes on the service's sensor callback thread
        runBlocking {
            repo.add(session)
        }
    }

}

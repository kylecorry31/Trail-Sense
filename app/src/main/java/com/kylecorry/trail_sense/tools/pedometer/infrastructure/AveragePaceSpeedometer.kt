package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import java.time.Duration
import java.time.Instant

class AveragePaceSpeedometer(
    private val stepTrackerService: IStepTrackerService,
    private val paceCalculator: IPaceCalculator
) : AbstractSensor(), ISpeedometer {

    private val timer = CoroutineTimer {
        val stepPeriod = stepTrackerService.getOpenStepTrackingPeriod() ?: run {
            reset()
            return@CoroutineTimer
        }
        speed = paceCalculator.speed(stepPeriod.steps, Duration.between(stepPeriod.startTime, Instant.now()))
        hasValidReading = true

        notifyListeners()
    }

    override var hasValidReading: Boolean = false
        private set

    override var speed: Speed = ZERO_SPEED
        private set

    override fun startImpl() {
        timer.interval(10000)
    }

    override fun stopImpl() {
        timer.stop()
    }

    private fun reset() {
        hasValidReading = false
        speed = ZERO_SPEED
    }

}

package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import java.time.Duration
import java.time.Instant

class AveragePaceSpeedometer(
    private val stepCounter: IStepCounter,
    private val paceCalculator: IPaceCalculator
) : AbstractSensor(), ISpeedometer {

    private val timer = Timer {
        val lastReset = stepCounter.startTime
        val steps = stepCounter.steps

        if (lastReset == null) {
            reset()
            return@Timer
        }

        speed = paceCalculator.speed(steps, Duration.between(lastReset, Instant.now()))
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
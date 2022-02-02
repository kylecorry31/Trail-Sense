package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import java.time.Duration
import java.time.Instant

class CurrentPaceSpeedometer(
    private val pedometer: IPedometer,
    private val paceCalculator: IPaceCalculator
) : AbstractSensor(), ISpeedometer {

    private val timer = Timer {
        updateSpeed()
    }

    override var hasValidReading: Boolean = false
        private set
    override var speed: Speed = ZERO_SPEED
        private set

    private var lastSteps = 0
    private var lastTime = Instant.MIN

    override fun startImpl() {
        lastTime = Instant.MIN
        lastSteps = 0
        hasValidReading = false
        speed = ZERO_SPEED
        pedometer.start(this::onPedometerUpdate)
        timer.interval(10000)
    }

    override fun stopImpl() {
        pedometer.stop(this::onPedometerUpdate)
        timer.stop()
    }

    private fun updateSpeed(){
        if (lastTime == Instant.MIN) {
            lastSteps = pedometer.steps
            lastTime = Instant.now()
            return
        }

        val steps = pedometer.steps - lastSteps
        val duration = Duration.between(lastTime, Instant.now())
        speed = paceCalculator.speed(steps.toLong(), duration)
        hasValidReading = true

        lastTime = Instant.now()
        lastSteps = pedometer.steps

        notifyListeners()
    }

    private fun onPedometerUpdate(): Boolean {
        return true
    }
}
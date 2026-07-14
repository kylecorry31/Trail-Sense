package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import java.time.Duration

class AveragePaceSpeedometer(
    private val stepTrackerService: IStepTrackerService,
    private val paceCalculator: IPaceCalculator,
    private val pedometerPreferences: IPedometerPreferences
) : AbstractSensor(), ISpeedometer {

    private val timer = CoroutineTimer {
        val stepPeriod = stepTrackerService.getOpenStepTrackingPeriod() ?: run {
            reset()
            return@CoroutineTimer
        }
        speed = paceCalculator.speed(stepPeriod.steps, getDuration(stepPeriod))
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

    private fun getDuration(period: StepTrackingPeriod): Duration {
        return when (pedometerPreferences.averagePaceTimeMode) {
            AveragePaceTimeMode.Active -> period.activeTime
            AveragePaceTimeMode.Elapsed -> period.elapsedTime
        }
    }

}

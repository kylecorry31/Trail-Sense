package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.os.Bundle
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class AveragePaceSpeedometer(
    private val stepTrackerService: IStepTrackerService,
    private val paceCalculator: IPaceCalculator,
    private val pedometerPreferences: IPedometerPreferences
) : AbstractSensor(), ISpeedometer {

    private val runner = CoroutineQueueRunner()

    private val timer = CoroutineTimer {
        refresh()
    }

    override var hasValidReading: Boolean = false
        private set

    override var speed: Speed = ZERO_SPEED
        private set

    override fun startImpl() {
        timer.interval(10000)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, this::onStepsChanged)
    }

    override fun stopImpl() {
        timer.stop()
        Tools.unsubscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, this::onStepsChanged)
        runner.cancel()
    }

    private fun reset() {
        hasValidReading = false
        speed = ZERO_SPEED
    }

    private suspend fun onStepsChanged(data: Bundle) {
        refresh()
    }

    private suspend fun refresh() {
        runner.enqueue {
            val stepPeriod = stepTrackerService.getOpenStepTrackingPeriod() ?: run {
                reset()
                return@enqueue
            }
            speed = paceCalculator.speed(stepPeriod.steps, getDuration(stepPeriod))
            hasValidReading = true

            notifyListeners()
        }
    }

    private fun getDuration(period: StepTrackingPeriod): Duration {
        return when (pedometerPreferences.averagePaceTimeMode) {
            AveragePaceTimeMode.Active -> period.activeTime
            AveragePaceTimeMode.Elapsed -> period.elapsedTime
        }
    }

}

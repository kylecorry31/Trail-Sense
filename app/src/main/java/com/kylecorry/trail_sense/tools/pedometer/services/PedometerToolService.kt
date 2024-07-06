package com.kylecorry.trail_sense.tools.pedometer.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class PedometerToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)

    override val id: String = PedometerToolRegistration.SERVICE_PEDOMETER

    override val name: String = context.getString(R.string.pedometer)

    override fun getFrequency(): Duration {
        return Duration.ZERO
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.pedometer.isEnabled
    }

    override fun isBlocked(): Boolean {
        return prefs.isLowPowerModeOn
    }

    override suspend fun enable() {
        prefs.pedometer.isEnabled = true
        Tools.broadcast(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED)
        restart()
    }

    override suspend fun disable() {
        prefs.pedometer.isEnabled = false
        Tools.broadcast(PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED)
        stop()
    }

    override suspend fun restart() {
        if (isEnabled() && !isBlocked()) {
            StepCounterService.start(context)
        } else {
            stop()
        }
    }

    override suspend fun stop() {
        StepCounterService.stop(context)
    }
}
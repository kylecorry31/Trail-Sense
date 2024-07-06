package com.kylecorry.trail_sense.tools.flashlight.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.flashlight.FlashlightToolRegistration
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import java.time.Duration

class FlashlightToolService(context: Context) : ToolService {

    private val subsystem = FlashlightSubsystem.getInstance(context)

    override val id: String = FlashlightToolRegistration.SERVICE_FLASHLIGHT

    override val name: String = context.getString(R.string.flashlight_title)

    override fun getFrequency(): Duration {
        return Duration.ZERO
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return subsystem.getMode() != FlashlightMode.Off
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        // Not expected to be called directly
        subsystem.set(FlashlightMode.Torch)
    }

    override suspend fun disable() {
        stop()
    }

    override suspend fun restart() {
        // Does not support restarting
    }

    override suspend fun stop() {
        subsystem.set(FlashlightMode.Off)
    }
}
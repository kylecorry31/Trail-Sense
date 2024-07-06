package com.kylecorry.trail_sense.tools.whitenoise.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.whitenoise.WhiteNoiseToolRegistration
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import java.time.Duration

class WhiteNoiseToolService(private val context: Context) : ToolService {
    override val id: String = WhiteNoiseToolRegistration.SERVICE_WHITE_NOISE

    override val name: String = context.getString(R.string.tool_white_noise_title)

    override fun getFrequency(): Duration {
        return Duration.ZERO
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return WhiteNoiseService.isRunning
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        WhiteNoiseService.start(context)
    }

    override suspend fun disable() {
        stop()
    }

    override suspend fun restart() {
        // Does not support restarting
    }

    override suspend fun stop() {
        WhiteNoiseService.stop(context)
    }
}
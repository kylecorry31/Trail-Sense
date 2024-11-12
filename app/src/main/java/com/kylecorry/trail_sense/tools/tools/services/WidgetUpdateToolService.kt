package com.kylecorry.trail_sense.tools.tools.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import java.time.Duration

class WidgetUpdateToolService(private val context: Context) : ToolService {

    override val id: String = SettingsToolRegistration.SERVICE_WIDGET_UPDATER

    override val name: String = context.getString(R.string.widget_updater)

    override fun getFrequency(): Duration {
        return Duration.ofHours(1)
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        restart()
    }

    override suspend fun disable() {
        stop()
    }

    override suspend fun restart() {
        if (isEnabled() && !isBlocked()) {
            WidgetUpdateWorker.start(context)
        }
    }

    override suspend fun stop() {
        WidgetUpdateWorker.stop(context)
    }
}
package com.kylecorry.trail_sense.tools.astronomy.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class SunriseAlertsToolService(private val context: Context): ToolService {

    private val prefs = UserPreferences(context)

    override val id: String = AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS

    override val name: String = context.getString(R.string.sunrise_alerts)

    override fun getFrequency(): Duration {
        return Duration.ofDays(1)
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.astronomy.sendSunriseAlerts
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        prefs.astronomy.sendSunriseAlerts = true
        Tools.broadcast(AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED)
        Tools.broadcast(AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_STATE_CHANGED)
        restart()
    }

    override suspend fun disable() {
        prefs.astronomy.sendSunriseAlerts = false
        Tools.broadcast(AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED)
        Tools.broadcast(AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_STATE_CHANGED)
        stop()
    }

    override suspend fun restart() {
        // Always starts - it short circuits if it doesn't need to run
        SunriseAlarmReceiver.start(context)
    }

    override suspend fun stop() {
        SunriseAlarmReceiver.scheduler(context).cancel()
    }
}
package com.kylecorry.trail_sense.tools.astronomy.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import java.time.Duration

class AstronomyAlertsToolService(private val context: Context) : ToolService {

    private val prefs = UserPreferences(context)

    override val id: String = AstronomyToolRegistration.SERVICE_ASTRONOMY_ALERTS

    override val name: String = context.getString(R.string.astronomy_alerts)

    override fun getFrequency(): Duration {
        return Duration.ofDays(1)
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.astronomy.sendAstronomyAlerts
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        // WHY: Not expected to be called, since users control the individual alerts
        prefs.astronomy.sendLunarEclipseAlerts = true
        prefs.astronomy.sendMeteorShowerAlerts = true
        prefs.astronomy.sendSolarEclipseAlerts = true
        restart()
    }

    override suspend fun disable() {
        prefs.astronomy.sendLunarEclipseAlerts = false
        prefs.astronomy.sendMeteorShowerAlerts = false
        prefs.astronomy.sendSolarEclipseAlerts = false
        stop()
    }

    override suspend fun restart() {
        AstronomyDailyWorker.start(context)
    }

    override suspend fun stop() {
        AstronomyDailyWorker.stop(context)
    }
}

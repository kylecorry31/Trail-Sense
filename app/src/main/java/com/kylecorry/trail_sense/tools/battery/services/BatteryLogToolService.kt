package com.kylecorry.trail_sense.tools.battery.services

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import java.time.Duration

class BatteryLogToolService(private val context: Context) : ToolService {
    private val prefs = UserPreferences(context)

    override val id: String = BatteryToolRegistration.SERVICE_BATTERY_LOG

    override val name: String = context.getString(R.string.pref_tiles_battery_log)

    override fun getFrequency(): Duration {
        return Duration.ofHours(1)
    }

    override fun isRunning(): Boolean {
        return isEnabled() && !isBlocked()
    }

    override fun isEnabled(): Boolean {
        return prefs.power.enableBatteryLog
    }

    override fun isBlocked(): Boolean {
        return false
    }

    override suspend fun enable() {
        prefs.power.enableBatteryLog = true
        restart()
    }

    override suspend fun disable() {
        prefs.power.enableBatteryLog = false
        stop()
    }

    override suspend fun restart() {
        BatteryLogWorker.enableBatteryLog(context, isEnabled() && !isBlocked())
    }

    override suspend fun stop() {
        BatteryLogWorker.enableBatteryLog(context, false)
    }
}
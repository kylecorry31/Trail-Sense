package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import com.kylecorry.andromeda.battery.IBattery
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.PowerService
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class BatteryService {

    private val powerService = PowerService()

    fun getRunningServices(context: Context): List<RunningService> {
        val tools = Tools.getTools(context)
        return tools
            .flatMap { it.services }
            .filter { it.isRunning() }
            .map {
                RunningService(
                    it.name,
                    it.getFrequency()
                ) {
                    it.disable()
                }
            }
    }

    fun getTimeUntilEmpty(battery: IBattery, readings: List<BatteryReading>): Duration? {
        val capacity = battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), false)
        val lastDischargeRate = rates.lastOrNull { it < 0f } ?: return null
        return powerService.getTimeUntilEmpty(capacity, lastDischargeRate)
    }

    fun getTimeUntilFull(battery: IBattery, readings: List<BatteryReading>): Duration? {
        val capacity = battery.percent
        val rates = powerService.getRates(readings, Duration.ofMinutes(5), false)
        val lastChargeRate = rates.lastOrNull { it > 0f } ?: return null
        val maxCapacity = 100f
        return powerService.getTimeUntilFull(capacity, maxCapacity, lastChargeRate)
    }


}
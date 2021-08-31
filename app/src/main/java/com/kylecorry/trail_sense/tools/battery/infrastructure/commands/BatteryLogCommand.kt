package com.kylecorry.trail_sense.tools.battery.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant

class BatteryLogCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() {
        val battery = Battery(context)
        val batteryRepo = BatteryRepo.getInstance(context)
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(Duration.ofSeconds(30).toMillis()) {
                battery.read()
            }
        }
        val pct = battery.percent
        val charging = battery.chargingStatus == BatteryChargingStatus.Charging
        val time = Instant.now()
        val capacity = battery.capacity
        val reading = BatteryReadingEntity(pct, capacity, charging, time)
        if (battery.hasValidReading) {
            batteryRepo.add(reading)
        }
        batteryRepo.deleteBefore(Instant.now().minus(Duration.ofDays(1)))
    }
}
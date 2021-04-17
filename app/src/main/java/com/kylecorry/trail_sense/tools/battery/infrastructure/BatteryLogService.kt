package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.Battery
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryChargingStatus
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.services.CoroutineService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class BatteryLogService: CoroutineService() {
    override suspend fun doWork() {
        acquireWakelock("BatteryLogService", Duration.ofSeconds(10))
        val battery = Battery(applicationContext)
        val batteryRepo = BatteryRepo.getInstance(applicationContext)
        withContext(Dispatchers.IO) {
            battery.read()
        }
        val pct = battery.percent
        val charging = battery.chargingStatus == BatteryChargingStatus.Charging
        val time = Instant.now()
        val capacity = battery.capacity
        val reading = BatteryReadingEntity(pct, capacity, charging, time)
        batteryRepo.add(reading)
        batteryRepo.deleteBefore(Instant.now().minus(Duration.ofDays(1)))
        BatteryLogWorker.scheduler(applicationContext).schedule(Duration.ofHours(1))
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, BatteryLogService::class.java)
        }

        fun start(context: Context) {
            IntentUtils.startService(context, intent(context), foreground = false)
        }
    }
}
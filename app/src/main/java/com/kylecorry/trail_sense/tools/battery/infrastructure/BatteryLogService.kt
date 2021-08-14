package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.services.CoroutineService
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.andromeda.core.system.IntentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class BatteryLogService: CoroutineService() {
    override suspend fun doWork() {
        acquireWakelock("BatteryLogService", Duration.ofSeconds(30))
        try {
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
        } finally {
            stopSelf()
        }
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, BatteryLogService::class.java)
        }

        fun start(context: Context) {
            try {
                IntentUtils.startService(context, intent(context), foreground = false)
            } catch (e: Exception){
                Log.e("BatteryLogService", "Could not start the battery service")
                e.printStackTrace()
            }
        }
    }
}
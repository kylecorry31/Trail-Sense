package com.kylecorry.trail_sense.shared

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ListenableWorker
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.jobs.OneTimeTaskSchedulerFactory
import com.kylecorry.trail_sense.astronomy.infrastructure.AstronomyDailyWorker
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackWorker
import com.kylecorry.trail_sense.navigation.paths.infrastructure.services.BacktrackAlwaysOnService
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.weather.infrastructure.WeatherMonitorAlwaysOnService
import com.kylecorry.trail_sense.weather.infrastructure.WeatherUpdateWorker
import java.time.Duration

object Background {

    const val WeatherMonitor = 2387092
    const val Backtrack = 7238542
    const val BatteryLogger = 2739852
    const val AstronomyAlerts = 72394823
    const val SunsetAlerts = 8309

    private val alwaysOnThreshold = Duration.ofMinutes(15)

    private val workers = mapOf<Int, Class<out ListenableWorker>>(
        WeatherMonitor to WeatherUpdateWorker::class.java,
        Backtrack to BacktrackWorker::class.java,
        BatteryLogger to BatteryLogWorker::class.java,
        AstronomyAlerts to AstronomyDailyWorker::class.java
    )

    private val foregroundServices = mapOf<Int, Class<out Service>>(
        WeatherMonitor to WeatherMonitorAlwaysOnService::class.java,
        Backtrack to BacktrackAlwaysOnService::class.java
    )

    private val alarms = mapOf<Int, Class<out BroadcastReceiver>>(
        SunsetAlerts to SunsetAlarmReceiver::class.java
    )

    /**
     * Start a background process
     * @param context the context
     * @param id the unique ID of the process
     * @param frequency the frequency that the process is run at - used to choose between a service and worker
     */
    fun start(context: Context, id: Int, frequency: Duration? = null) {

        val hasWorker = workers.containsKey(id)
        val hasService = foregroundServices.containsKey(id)
        val hasAlarm = alarms.containsKey(id)

        if (hasWorker) {
            if (hasService && frequency != null && frequency < alwaysOnThreshold) {
                // This needs to run as an always on service since the frequency is too small for a worker to pick up
                stopWorker(context, id)
                startService(context, id)
            } else {
                // Updates are infrequent enough to run as a worker or there is no service
                stopService(context, id)
                startWorker(context, id)
            }
        } else if (hasService) {
            // No worker, but there is a service
            startService(context, id)
        } else if (hasAlarm) {
            startAlarm(context, id)
        }
    }

    /**
     * Stop a background process
     * @param context the context
     * @param id the unique ID of the process
     */
    fun stop(context: Context, id: Int) {
        stopWorker(context, id)
        stopService(context, id)
        stopAlarm(context, id)
    }

    private fun startAlarm(context: Context, id: Int) {
        createAlarmScheduler(context, id)?.once()
    }

    private fun stopAlarm(context: Context, id: Int) {
        createAlarmScheduler(context, id)?.cancel()
    }

    private fun startService(context: Context, id: Int) {
        val service = foregroundServices[id] ?: return
        Intents.startService(context, Intent(context, service), foreground = true)
    }

    private fun stopService(context: Context, id: Int) {
        val service = foregroundServices[id] ?: return
        context.stopService(Intent(context, service))
    }

    private fun startWorker(context: Context, id: Int) {
        createWorkScheduler(context, id)?.once()
    }

    private fun stopWorker(context: Context, id: Int) {
        createWorkScheduler(context, id)?.cancel()
    }

    private fun createAlarmScheduler(context: Context, id: Int): IOneTimeTaskScheduler? {
        val alarm = alarms[id] ?: return null
        return OneTimeTaskSchedulerFactory(context).exact(alarm, id)
    }

    private fun createWorkScheduler(
        context: Context,
        id: Int
    ): IOneTimeTaskScheduler? {
        val worker = workers[id] ?: return null
        return OneTimeTaskSchedulerFactory(context).deferrable(worker, id)
    }

}
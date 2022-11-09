package com.kylecorry.trail_sense.shared

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ListenableWorker
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.jobs.OneTimeTaskSchedulerFactory
import com.kylecorry.andromeda.services.ForegroundService
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

    private val tasks = mapOf<Int, List<Class<*>>>(
        WeatherMonitor to listOf(
            WeatherUpdateWorker::class.java,
            WeatherMonitorAlwaysOnService::class.java
        ),
        Backtrack to listOf(BacktrackWorker::class.java, BacktrackAlwaysOnService::class.java),
        BatteryLogger to listOf(BatteryLogWorker::class.java),
        AstronomyAlerts to listOf(AstronomyDailyWorker::class.java),
        SunsetAlerts to listOf(SunsetAlarmReceiver::class.java)
    )

    /**
     * Start a background process
     * @param context the context
     * @param id the unique ID of the process
     * @param frequency the frequency that the process is run at - used to choose between a service and worker
     */
    fun start(context: Context, id: Int, frequency: Duration? = null) {
        val hasWorker = getTask(id, ListenableWorker::class.java) != null
        val hasService = getTask(id, Service::class.java) != null
        val hasAlarm = getTask(id, BroadcastReceiver::class.java) != null

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
        val foregroundService = getTask(id, ForegroundService::class.java)
        val service = getTask(id, Service::class.java) ?: return
        Intents.startService(
            context,
            Intent(context, foregroundService ?: service),
            foreground = foregroundService != null
        )
    }

    private fun stopService(context: Context, id: Int) {
        val service = getTask(id, Service::class.java) ?: return
        context.stopService(Intent(context, service))
    }

    private fun startWorker(context: Context, id: Int) {
        createWorkScheduler(context, id)?.once()
    }

    private fun stopWorker(context: Context, id: Int) {
        createWorkScheduler(context, id)?.cancel()
    }

    private fun createAlarmScheduler(context: Context, id: Int): IOneTimeTaskScheduler? {
        val alarm = getTask(id, BroadcastReceiver::class.java) ?: return null
        return OneTimeTaskSchedulerFactory(context).exact(alarm, id)
    }

    private fun createWorkScheduler(
        context: Context,
        id: Int
    ): IOneTimeTaskScheduler? {
        val worker = getTask(id, ListenableWorker::class.java) ?: return null
        return OneTimeTaskSchedulerFactory(context).deferrable(worker, id)
    }

    private fun <T> getTask(id: Int, type: Class<out T>): Class<out T>? {
        val availableTasks = tasks[id] ?: return null
        val task = availableTasks.firstOrNull { type.isAssignableFrom(it) } ?: return null
        return task as Class<out T>
    }

}
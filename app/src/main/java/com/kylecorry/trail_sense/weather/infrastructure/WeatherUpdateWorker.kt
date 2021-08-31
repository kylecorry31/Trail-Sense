package com.kylecorry.trail_sense.weather.infrastructure

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.jobs.WorkTaskScheduler
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.infrastructure.commands.MonitorWeatherCommand
import java.time.LocalDateTime

class WeatherUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d(javaClass.simpleName, "Started")
        try {
            MonitorWeatherCommand(applicationContext).execute()
        } catch (e: Exception) {
            throw e
        } finally {
            val frequency = UserPreferences(applicationContext).weather.weatherUpdateFrequency
            scheduler(applicationContext).schedule(frequency)
            Log.d(javaClass.simpleName, "Scheduled next job at ${LocalDateTime.now().plus(frequency)}")

        }
        return Result.success()
    }

    companion object {
        private const val WORK_TAG = "com.kylecorry.trail_sense.WeatherUpdateWorker"

        fun scheduler(context: Context): ITaskScheduler {
            return WorkTaskScheduler(
                context,
                WeatherUpdateWorker::class.java,
                WORK_TAG,
                false
            )
        }
    }

}
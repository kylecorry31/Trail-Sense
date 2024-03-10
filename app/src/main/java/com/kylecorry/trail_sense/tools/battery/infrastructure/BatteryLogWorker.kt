package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.PeriodicTaskSchedulerFactory
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.BatteryLogCommand
import java.time.Duration

class BatteryLogWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        BatteryLogCommand(applicationContext).execute()
        return Result.success()
    }

    companion object {

        private const val UNIQUE_ID = 2739852

        /**
         * This enable battery log worker to runs about every hour in order to accurately show battery drain
         *
         * enabled: Boolean: false cancel the process
         * enabled: Boolean: true start the process
         */
        fun enableBatteryLog(context: Context, enabled: Boolean) {
            if(enabled) {
                scheduler(context).interval(Duration.ofHours(1))
            }else {
                scheduler(context).cancel()
            }
        }

        private fun scheduler(context: Context): IPeriodicTaskScheduler {
            return PeriodicTaskSchedulerFactory(context).deferrable(
                BatteryLogWorker::class.java,
                UNIQUE_ID
            )
        }
    }
}
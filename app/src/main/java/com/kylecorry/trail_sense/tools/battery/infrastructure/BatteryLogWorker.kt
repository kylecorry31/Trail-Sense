package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.jobs.OneTimeTaskSchedulerFactory
import com.kylecorry.trail_sense.shared.VariableIntervalWorker
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.BatteryLogCommand
import java.time.Duration

class BatteryLogWorker(context: Context, params: WorkerParameters) :
    VariableIntervalWorker(context, params, wakelockDuration = Duration.ofSeconds(15)) {

    override fun isEnabled(context: Context): Boolean {
        return true
    }

    override fun getFrequency(context: Context): Duration {
        return Duration.ofHours(1)
    }

    override suspend fun execute(context: Context) {
        BatteryLogCommand(context).execute()
    }

    override val uniqueId: Int = UNIQUE_ID

    companion object {

        private const val UNIQUE_ID = 2739852

        fun scheduler(context: Context): IOneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).deferrable(
                BatteryLogWorker::class.java,
                UNIQUE_ID
            )
        }
    }

}
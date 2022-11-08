package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.IntervalWorker
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.tools.battery.infrastructure.commands.BatteryLogCommand
import java.time.Duration

class BatteryLogWorker(context: Context, params: WorkerParameters) :
    IntervalWorker(context, params, wakelockDuration = Duration.ofSeconds(15)) {

    override fun getFrequency(context: Context): Duration {
        return Duration.ofHours(1)
    }

    override suspend fun execute(context: Context) {
        BatteryLogCommand(context).execute()
    }

    override val uniqueId: Int = Background.BatteryLogger
}
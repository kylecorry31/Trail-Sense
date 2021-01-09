package com.kylecorry.trail_sense.tools.backtrack.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.tasks.ITaskScheduler
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.services.BacktrackService
import java.time.Duration

object BacktrackScheduler {
    fun start(context: Context) {
        val scheduler = getScheduler(context)
        scheduler.schedule(Duration.ZERO)
    }

    fun stop(context: Context) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        context.stopService(BacktrackService.intent(context))
    }

    fun getScheduler(context: Context): ITaskScheduler {
        return BacktrackWorker.scheduler(context)
    }
}
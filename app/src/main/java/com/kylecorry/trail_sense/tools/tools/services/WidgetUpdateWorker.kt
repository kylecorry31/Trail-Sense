package com.kylecorry.trail_sense.tools.tools.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.PeriodicTaskSchedulerFactory
import com.kylecorry.trail_sense.shared.widgets.WidgetUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Tools.getTools(applicationContext).flatMap { it.widgets }.forEach {
            WidgetUtils.triggerUpdate(applicationContext, it.widgetClass)
        }
        return Result.success()
    }

    companion object {

        private const val UNIQUE_ID = 267389

        fun start(context: Context) {
            scheduler(context).interval(Duration.ofMinutes(30))
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        private fun scheduler(context: Context): IPeriodicTaskScheduler {
            return PeriodicTaskSchedulerFactory(context).deferrable(
                WidgetUpdateWorker::class.java,
                UNIQUE_ID
            )
        }
    }
}
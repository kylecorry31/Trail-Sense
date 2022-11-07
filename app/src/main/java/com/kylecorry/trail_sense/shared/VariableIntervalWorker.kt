package com.kylecorry.trail_sense.shared

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.core.system.Wakelocks
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.jobs.IOneTimeTaskScheduler
import com.kylecorry.andromeda.jobs.OneTimeTaskSchedulerFactory
import java.time.Duration
import java.time.LocalDateTime

// TODO: Prevent it from running too frequently
abstract class VariableIntervalWorker(
    context: Context,
    params: WorkerParameters,
    private val wakelockDuration: Duration? = null
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val foregroundInfo = getForegroundInfo(applicationContext)
        if (foregroundInfo != null) {
            setForeground(foregroundInfo)
        }

        val wakelock = if (wakelockDuration != null) {
            val wakelockTag = "$uniqueId::interval::wakelock"
            tryOrDefault(null) {
                Wakelocks.get(applicationContext, wakelockTag)
                    ?.also { it.acquire(wakelockDuration.toMillis()) }
            }
        } else {
            null
        }

        Log.d(javaClass.simpleName, "Started")
        try {
            execute(applicationContext)
        } finally {
            try {
                if (isEnabled(applicationContext)) {
                    setAlarm(applicationContext, getFrequency(applicationContext))
                }
            } finally {
                wakelock?.release()
            }
        }
        return Result.success()
    }

    protected abstract fun isEnabled(context: Context): Boolean
    protected abstract fun getFrequency(context: Context): Duration
    protected abstract suspend fun execute(context: Context)
    protected abstract val uniqueId: Int

    protected open fun getForegroundInfo(context: Context): ForegroundInfo? {
        return null
    }

    protected open fun getScheduler(context: Context): IOneTimeTaskScheduler {
        return OneTimeTaskSchedulerFactory(context).deferrable(this::class.java, uniqueId)
    }

    private fun setAlarm(context: Context, delay: Duration) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        scheduler.once(delay)
        Log.d(
            javaClass.simpleName,
            "Scheduled the next run for ${LocalDateTime.now().plus(delay)}"
        )
    }
}
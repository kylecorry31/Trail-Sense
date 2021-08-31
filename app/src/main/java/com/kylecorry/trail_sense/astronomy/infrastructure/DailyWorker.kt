package com.kylecorry.trail_sense.astronomy.infrastructure

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.core.time.toZonedDateTime
import com.kylecorry.andromeda.jobs.DeferredTaskScheduler
import com.kylecorry.andromeda.jobs.ITaskScheduler
import com.kylecorry.andromeda.preferences.Preferences
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

abstract class DailyWorker(
    context: Context,
    params: WorkerParameters,
    private val expedited: Boolean = false,
    private val tolerance: Duration = Duration.ofMinutes(30)
) : CoroutineWorker(context, params) {

    private val lock = Object()

    override suspend fun doWork(): Result {
        val context = applicationContext

        val jobState = synchronized(lock) {
            val now = LocalDateTime.now()
            val cache = Preferences(context)
            val lastRun = cache.getLocalDate(getLastRunKey(context))
            val shouldSend = isEnabled(context) && lastRun != now.toLocalDate()

            val sendTime = LocalDate.now().atTime(getScheduledTime(context))
            val tomorrowSendTime =
                LocalDate.now().plusDays(1).atTime(getScheduledTime(context))

            val sendWindowStart = sendTime - tolerance
            val sendWindowEnd = sendTime + tolerance

            val inWindow = now.isAfter(sendWindowStart) && now.isBefore(sendWindowEnd)
            val isTooEarly = now.isBefore(sendWindowStart)
            val isAfterWindow = now.isAfter(sendWindowEnd)

            if (inWindow && shouldSend) {
                Log.d(
                    javaClass.simpleName,
                    "Received a broadcast and executed"
                )
                cache.putLocalDate(getLastRunKey(context), now.toLocalDate())
                return@synchronized true to tomorrowSendTime
            }

            if (isTooEarly) {
                Log.d(
                    javaClass.simpleName,
                    "Received a broadcast too early"
                )
                return@synchronized false to sendTime
            }

            if (isAfterWindow || (inWindow && !shouldSend)) {
                Log.d(
                    javaClass.simpleName,
                    "Received a broadcast too late, it already ran today, or it is not enabled"
                )
            }

            return@synchronized false to tomorrowSendTime
        }

        try {
            if (jobState.first) {
                val foregroundInfo = getForegroundInfo(context)
                if (foregroundInfo != null) {
                    setForeground(foregroundInfo)
                }
                execute(context)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            setAlarm(context, jobState.second)
        }

        return Result.success()
    }

    protected abstract fun isEnabled(context: Context): Boolean
    protected abstract fun getScheduledTime(context: Context): LocalTime
    protected abstract fun getLastRunKey(context: Context): String
    protected abstract suspend fun execute(context: Context)
    protected abstract val uniqueId: String

    protected open fun getForegroundInfo(context: Context): ForegroundInfo? {
        return null
    }

    protected open fun getScheduler(context: Context): ITaskScheduler {
        return DeferredTaskScheduler(context, this::class.java, uniqueId)
    }

    private fun setAlarm(context: Context, time: LocalDateTime) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        scheduler.schedule(time.toZonedDateTime().toInstant())
        Log.d(
            javaClass.simpleName,
            "Scheduled the next run for $time"
        )
    }
}
package com.kylecorry.trail_sense.shared.tasks

import android.app.PendingIntent
import android.content.Context
import com.kylecorry.trailsensecore.infrastructure.system.AlarmUtils
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

class ExactTaskScheduler(private val context: Context, private val task: PendingIntent) :
    ITaskScheduler {


    override fun schedule(delay: Duration) {
        AlarmUtils.set(
            context,
            LocalDateTime.now().plus(delay),
            task,
            exact = true,
            allowWhileIdle = true
        )
    }

    override fun schedule(time: Instant) {
        schedule(Duration.between(Instant.now(), time))
    }

    override fun cancel() {
        AlarmUtils.cancel(context, task)
    }
}
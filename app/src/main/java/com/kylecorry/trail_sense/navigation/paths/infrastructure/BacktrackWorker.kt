package com.kylecorry.trail_sense.navigation.paths.infrastructure

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.jobs.IntervalWorker
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.navigation.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.shared.Background
import com.kylecorry.trail_sense.shared.FeatureState
import java.time.Duration

class BacktrackWorker(context: Context, params: WorkerParameters) :
    IntervalWorker(context, params, wakelockDuration = Duration.ofSeconds(15)) {

    override fun isEnabled(context: Context): Boolean {
        return BacktrackSubsystem.getInstance(context).getState() == FeatureState.On
    }

    override fun getFrequency(context: Context): Duration {
        return BacktrackSubsystem.getInstance(context).getFrequency()
    }

    override suspend fun execute(context: Context) {
        BacktrackCommand(applicationContext).execute()
    }

    override val uniqueId: Int = Background.Backtrack

    override fun getForegroundInfo(context: Context): ForegroundInfo? {
        if (!requiresForeground()) {
            return null
        }
        return ForegroundInfo(
            73922,
            Notify.background(
                applicationContext,
                NotificationChannels.CHANNEL_BACKGROUND_UPDATES,
                applicationContext.getString(R.string.notification_backtrack_update_title),
                applicationContext.getString(R.string.notification_backtrack_update_content),
                R.drawable.ic_update
            ),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )
    }

    private fun requiresForeground(): Boolean {
        return BacktrackRequiresForeground().isSatisfiedBy(applicationContext)
    }
}
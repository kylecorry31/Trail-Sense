package com.kylecorry.trail_sense.navigation.paths.infrastructure.services

import android.app.Notification
import com.kylecorry.andromeda.core.coroutines.SingleRunner
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.services.CoroutineIntervalService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.navigation.paths.infrastructure.receivers.StopBacktrackReceiver
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration

class BacktrackAlwaysOnService : CoroutineIntervalService(TAG) {
    private val prefs by lazy { UserPreferences(applicationContext) }
    private val formatService by lazy { FormatService(this) }

    private val backtrackCommand by lazy {
        BacktrackCommand(this)
    }

    override val foregroundNotificationId: Int
        get() = 578879

    override val period: Duration
        get() = prefs.backtrackRecordFrequency

    private val runner = SingleRunner()

    override fun getForegroundNotification(): Notification {
        val openAction = NavigationUtils.pendingIntent(this, R.id.fragmentBacktrack)

        val stopAction = Notify.action(
            getString(R.string.stop),
            StopBacktrackReceiver.pendingIntent(this),
            R.drawable.ic_cancel
        )

        return Notify.persistent(
            this,
            FOREGROUND_CHANNEL_ID,
            getString(R.string.backtrack),
            getString(
                R.string.backtrack_high_priority_notification,
                formatService.formatDuration(prefs.backtrackRecordFrequency, includeSeconds = true)
            ),
            R.drawable.ic_tool_backtrack,
            intent = openAction,
            actions = listOf(stopAction),
            showForegroundImmediate = true
        )
    }

    override suspend fun doWork() {
        runner.single({
            backtrackCommand.execute()
        })
    }

    override fun onDestroy() {
        runner.cancel()
        stopService(true)
        super.onDestroy()
    }

    companion object {
        const val TAG = "BacktrackHighPriorityService"
        const val FOREGROUND_CHANNEL_ID = "Backtrack"
    }

}
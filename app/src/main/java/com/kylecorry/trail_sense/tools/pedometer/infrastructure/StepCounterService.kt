package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.services.ForegroundService
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.pedometer.domain.DailyStepResetCommand

class StepCounterService : ForegroundService() {

    private val pedometer by lazy { Pedometer(this) }
    private val counter by lazy { StepCounter(Preferences(this)) }
    private val formatService by lazy { FormatService(this) }
    private val prefs by lazy { UserPreferences(this) }
    private val dailyResetCommand: Command by lazy { DailyStepResetCommand(prefs.pedometer, counter) }

    private var lastSteps = -1

    override fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int {
        pedometer.start(this::onPedometer)
        return START_STICKY_COMPATIBILITY
    }

    override fun getForegroundNotification(): Notification {
        return getNotification()
    }

    private fun onPedometer(): Boolean {
        if (lastSteps == -1) {
            lastSteps = pedometer.steps
        }

        dailyResetCommand.execute()

        val newSteps = pedometer.steps - lastSteps
        counter.addSteps(newSteps.toLong())
        lastSteps = pedometer.steps
        Notify.send(this, NOTIFICATION_ID, getNotification())
        return true
    }

    override fun onDestroy() {
        pedometer.stop(this::onPedometer)
        stopService(true)
        super.onDestroy()
    }

    override val foregroundNotificationId: Int = NOTIFICATION_ID

    private fun getNotification(): Notification {
        val steps = counter.steps
        val stride = prefs.pedometer.strideLength.meters().distance
        val units = prefs.baseDistanceUnits
        val distance = Distance.meters(steps * stride).convertTo(units).toRelativeDistance()

        val openIntent = NavigationUtils.pendingIntent(this, R.id.fragmentToolPedometer)

        return Notify.persistent(
            this,
            CHANNEL_ID,
            getString(R.string.pedometer),
            formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            ),
            R.drawable.steps,
            intent = openIntent,
            group = NotificationChannels.GROUP_PEDOMETER
        )
    }

    companion object {
        const val CHANNEL_ID = "pedometer"
        const val NOTIFICATION_ID = 1279812

        fun intent(context: Context): Intent {
            return Intent(context, StepCounterService::class.java)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

        fun start(context: Context) {
            if (UserPreferences(context).isLowPowerModeOn){
                return
            }

            if (!Permissions.canRecognizeActivity(context)) {
                return
            }

            if (Notify.isActive(context, NOTIFICATION_ID)) {
                return
            }

            Intents.startService(context, intent(context), true)
        }

    }

}
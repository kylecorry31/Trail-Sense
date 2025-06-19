package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.background.services.AndromedaService
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem

class StepCounterService : AndromedaService() {

    private val pedometer by lazy { SensorService(this).getPedometer() }
    private val counter by lazy { StepCounter(PreferencesSubsystem.getInstance(this).preferences) }
    private val formatService by lazy { FormatService.getInstance(this) }
    private val prefs by lazy { UserPreferences(this) }
    private val commandFactory by lazy { PedometerCommandFactory(this) }
    private val dailyResetCommand: Command by lazy { commandFactory.getDailyStepReset() }
    private val distanceAlertCommand: Command by lazy { commandFactory.getDistanceAlert() }
    private val subsystem by lazy { PedometerSubsystem.getInstance(this) }

    private var lastSteps = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val flag = super.onStartCommand(intent, flags, startId)
        isRunning = true
        pedometer.start(this::onPedometer)
        return flag
    }

    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, getNotification())
    }

    private fun onPedometer(): Boolean {
        if (lastSteps == -1) {
            lastSteps = pedometer.steps
        }

        dailyResetCommand.execute()

        val newSteps = pedometer.steps - lastSteps
        counter.addSteps(newSteps.toLong())
        lastSteps = pedometer.steps
        AppServiceRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, getNotification())

        distanceAlertCommand.execute()
        return true
    }

    override fun onDestroy() {
        isRunning = false
        pedometer.stop(this::onPedometer)
        stopService(true)
        super.onDestroy()
    }

    private fun getNotification(): Notification {
        val distance = subsystem.distance.value.orElseGet { Distance(0f, DistanceUnits.Meters) }
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()

        val openIntent = NavigationUtils.pendingIntent(this, R.id.fragmentToolPedometer)
        val stopIntent = Intent(this, StopPedometerReceiver::class.java)
        val stopPendingIntent =
            PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = Notify.action(
            getString(R.string.stop),
            stopPendingIntent,
            R.drawable.ic_cancel
        )

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
            group = NOTIFICATION_GROUP_PEDOMETER,
            showForegroundImmediate = true,
            actions = listOf(stopAction)
        )
    }

    companion object {
        const val CHANNEL_ID = "pedometer"
        const val NOTIFICATION_ID = 1279812
        private const val NOTIFICATION_GROUP_PEDOMETER = "trail_sense_pedometer"


        var isRunning = false
            private set

        fun intent(context: Context): Intent {
            return Intent(context.applicationContext, StepCounterService::class.java)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

        fun isOn(): Boolean {
            return isRunning
        }

        fun start(context: Context) {
            if (UserPreferences(context).isLowPowerModeOn) {
                return
            }

            if (!Permissions.canRecognizeActivity(context)) {
                return
            }

            if (isOn()) {
                return
            }

            tryStartForegroundOrNotify(context) {
                Intents.startService(context.applicationContext, intent(context), true)
            }
        }

    }

}
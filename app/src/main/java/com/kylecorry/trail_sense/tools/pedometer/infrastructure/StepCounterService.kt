package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kylecorry.andromeda.background.services.AndromedaService
import com.kylecorry.andromeda.background.services.ForegroundInfo
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.tryStartForegroundOrNotify
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class StepCounterService : AndromedaService() {

    private val pedometer by lazy { SensorService(this).getPedometer() }
    private val stepTrackerService = getAppService<StepTrackerService>()
    private val formatService by lazy { FormatService.getInstance(this) }
    private val prefs by lazy { UserPreferences(this) }
    private val commandFactory by lazy { PedometerCommandFactory(this) }
    private val dailyResetCommand: CoroutineCommand by lazy { commandFactory.getDailyStepReset() }
    private val distanceAlertCommand: CoroutineCommand by lazy { commandFactory.getDistanceAlert() }
    private val notificationSubsystem = getAppService<NotificationSubsystem>()

    private val addStepsQueue = CoroutineQueueRunner()

    private val addStepsTask = BackgroundTask {
        addStepsQueue.enqueue {
            val currentSteps = pedometer.steps
            if (lastSteps == -1) {
                lastSteps = currentSteps
            }

            dailyResetCommand.execute()

            val newSteps = currentSteps - lastSteps
            stepTrackerService.addSteps(newSteps.toLong())
            lastSteps = currentSteps
            distanceAlertCommand.execute()
        }
    }

    private var lastSteps = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val flag = super.onStartCommand(intent, flags, startId)
        isRunning = true
        pedometer.start(this::onPedometer)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, this::onStepsChanged)
        return flag
    }

    override fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, getNotification(Distance.meters(0f)))
    }

    private fun onPedometer(): Boolean {
        addStepsTask.start()
        return true
    }

    private suspend fun onStepsChanged(data: Bundle?) {
        val steps = data?.getLong(PedometerToolRegistration.BROADCAST_PARAM_STEPS) ?: 0L
        val paceCalculator = StrideLengthPaceCalculator(prefs.pedometer.strideLength)
        val distance = paceCalculator.distance(steps)
        notificationSubsystem.send(NOTIFICATION_ID, getNotification(distance))
    }

    override fun onDestroy() {
        isRunning = false
        pedometer.stop(this::onPedometer)
        stopService(true)
        addStepsTask.stop()
        addStepsQueue.cancel()
        Tools.unsubscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, this::onStepsChanged)
        super.onDestroy()
    }

    private fun getNotification(distance: Distance): Notification {
        val convertedDistance = distance
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
                convertedDistance,
                Units.getDecimalPlaces(convertedDistance.units),
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

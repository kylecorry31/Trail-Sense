package com.kylecorry.trail_sense.tools.speedometer.infrastructure

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import com.kylecorry.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.IsLargeUnitSpecification
import com.kylecorry.trailsensecore.infrastructure.sensors.pedometer.Pedometer
import com.kylecorry.trailsensecore.infrastructure.services.ForegroundService
import com.kylecorry.trailsensecore.infrastructure.system.IntentUtils
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils

class PedometerService : ForegroundService() {

    private val notify by lazy { Notify(this) }
    private val pedometer by lazy { Pedometer(this) }
    private val sensorService by lazy { SensorService(this) }
    private val odometer by lazy { sensorService.getOdometer() }
    private val formatService by lazy { FormatServiceV2(this) }
    private val prefs by lazy { UserPreferences(this) }

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

        val newSteps = pedometer.steps - lastSteps
        odometer.addDistance(Distance.meters(prefs.strideLength.meters().distance * newSteps))
        lastSteps = pedometer.steps
        notify.send(NOTIFICATION_ID, getNotification())
        return true
    }

    override fun onDestroy() {
        pedometer.stop(this::onPedometer)
        stopService(true)
        super.onDestroy()
    }

    override val foregroundNotificationId: Int = NOTIFICATION_ID

    private fun getNotification(): Notification {
        val units = prefs.baseDistanceUnits
        val distance = odometer.distance.convertTo(units).toRelativeDistance()

        val openIntent = NavigationUtils.pendingIntent(this, R.id.fragmentToolSpeedometer)

        return notify.persistent(
            CHANNEL_ID,
            getString(R.string.odometer),
            formatService.formatDistance(
                distance,
                if (IsLargeUnitSpecification().isSatisfiedBy(distance.units)) 2 else 0
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
            return Intent(context, PedometerService::class.java)
        }

        fun stop(context: Context) {
            context.stopService(intent(context))
        }

        fun start(context: Context) {
            if (UserPreferences(context).isLowPowerModeOn){
                return
            }

            if (!PermissionUtils.hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)) {
                return
            }

            val notify = Notify(context)
            if (notify.isActive(NOTIFICATION_ID)) {
                return
            }

            IntentUtils.startService(context, intent(context), true)
        }

    }

}
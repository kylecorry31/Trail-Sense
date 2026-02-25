package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.diagnostics.status.GpsStatusBadgeProvider
import com.kylecorry.trail_sense.tools.diagnostics.status.SensorStatusBadgeProvider

class SensorStatusBadgeView(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val gpsBadge: Badge
    private val compassBadge: Badge

    private var gpsStatusBadgeProvider: GpsStatusBadgeProvider? = null
    private var compassStatusBadgeProvider: SensorStatusBadgeProvider? = null

    private var gps: ISatelliteGPS? = null
    private var compassSensor: ISensor? = null

    private var isStarted = false

    private val updateTimer = CoroutineTimer {
        updateBadges()
    }

    private val lifecycleListener = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> start()
            Lifecycle.Event.ON_PAUSE -> stop()
            else -> {}
        }
    }

    init {
        orientation = VERTICAL
        gravity = android.view.Gravity.START

        inflate(context, R.layout.view_sensor_status_badges, this)

        gpsBadge = findViewById(R.id.gps_status)
        compassBadge = findViewById(R.id.compass_status)

        if (!SensorService(context).hasCompass()) {
            compassBadge.visibility = GONE
        }

        doOnAttach {
            findViewTreeLifecycleOwner()?.lifecycle?.addObserver(lifecycleListener)
        }

        doOnDetach {
            findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(lifecycleListener)
            stop()
        }
    }

    fun setSensors(gps: ISatelliteGPS, compass: ISensor) {
        this.gps = gps
        this.compassSensor = compass
        gpsStatusBadgeProvider = GpsStatusBadgeProvider(gps, context)
        compassStatusBadgeProvider = SensorStatusBadgeProvider(compass, context, R.drawable.ic_compass_icon)
        // Immediately update so badges don't show stale data
        updateBadges()
    }

    fun getSensors(): List<ISensor> {
        return listOfNotNull(gps, compassSensor)
    }

    private fun start() {
        if (isStarted) return
        isStarted = true
        updateTimer.interval(1000)
    }

    private fun stop() {
        if (!isStarted) return
        isStarted = false
        updateTimer.stop()
    }

    private fun updateBadges() {
        gpsStatusBadgeProvider?.getBadge()?.let {
            gpsBadge.setStatusText(it.name)
            gpsBadge.setBackgroundTint(it.color)
        }
        compassStatusBadgeProvider?.getBadge()?.let {
            compassBadge.setStatusText(it.name)
            compassBadge.setBackgroundTint(it.color)
        }
    }
}

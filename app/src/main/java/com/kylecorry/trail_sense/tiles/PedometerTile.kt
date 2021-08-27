package com.kylecorry.trail_sense.tiles

import android.hardware.Sensor
import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.speedometer.infrastructure.PedometerService

@RequiresApi(Build.VERSION_CODES.N)
class PedometerTile : AndromedaTileService() {

    private val prefs by lazy { UserPreferences(this) }
    private val formatService by lazy { FormatService(this) }
    private val odometer by lazy { SensorService(this).getOdometer() }

    override fun isOn(): Boolean {
        return prefs.usePedometer && !isDisabled()
    }

    override fun isDisabled(): Boolean {
        val hasPermission = Permissions.canRecognizeActivity(this)
        return !Sensors.hasSensor(this, Sensor.TYPE_STEP_COUNTER) || !hasPermission || prefs.isLowPowerModeOn
    }

    override fun onInterval() {
        val units = prefs.baseDistanceUnits
        val distance = odometer.distance.convertTo(units).toRelativeDistance()
        setSubtitle(formatService.formatDistance(distance))
    }

    override fun start() {
        prefs.usePedometer = true
        PedometerService.start(this)
    }

    override fun stop() {
        prefs.usePedometer = false
        PedometerService.stop(this)
    }
}
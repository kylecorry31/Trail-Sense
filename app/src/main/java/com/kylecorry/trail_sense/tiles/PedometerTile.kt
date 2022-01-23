package com.kylecorry.trail_sense.tiles

import android.hardware.Sensor
import android.os.Build
import androidx.annotation.RequiresApi
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.services.AndromedaTileService
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.PedometerService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService

@RequiresApi(Build.VERSION_CODES.N)
class PedometerTile : AndromedaTileService() {

    private val prefs by lazy { UserPreferences(this) }
    private val formatService by lazy { FormatService(this) }
    private val counter by lazy { StepCounter(Preferences(this)) }
    private val pedometerService = PedometerService()

    override fun isOn(): Boolean {
        return prefs.pedometer.isEnabled && !isDisabled()
    }

    override fun isDisabled(): Boolean {
        val hasPermission = Permissions.canRecognizeActivity(this)
        return !Sensors.hasSensor(this, Sensor.TYPE_STEP_COUNTER) || !hasPermission || prefs.isLowPowerModeOn
    }

    override fun onInterval() {
        setSubtitle(formatService.formatDistance(getDistance()))
    }

    override fun start() {
        prefs.pedometer.isEnabled = true
        StepCounterService.start(this)
    }

    override fun stop() {
        prefs.pedometer.isEnabled = false
        StepCounterService.stop(this)
    }

    private fun getDistance(): Distance {
        val stride = prefs.pedometer.strideLength.meters()
        val units = prefs.baseDistanceUnits
        val distance = pedometerService.getDistance(counter.steps, stride)
        return distance.convertTo(units).toRelativeDistance()
    }

}
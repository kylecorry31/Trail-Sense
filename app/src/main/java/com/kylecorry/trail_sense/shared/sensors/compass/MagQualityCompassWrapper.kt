package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import kotlin.math.min

class MagQualityCompassWrapper(private val compass: ICompass, private val magnetometer: ISensor) :
    AbstractSensor(), ICompass {
    override val bearing: Bearing
        get() = compass.bearing

    override var declination: Float
        get() = compass.declination
        set(value) {
            compass.declination = value
        }
    override val hasValidReading: Boolean
        get() = compass.hasValidReading

    override val rawBearing: Float
        get() = compass.rawBearing

    override val quality: Quality
        get() = Quality.values()[min(magnetometer.quality.ordinal, compass.quality.ordinal)]

    override fun startImpl() {
        compass.start(this::onReading)
        magnetometer.start(this::onReading)
    }

    override fun stopImpl() {
        compass.stop(this::onReading)
        magnetometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }
}
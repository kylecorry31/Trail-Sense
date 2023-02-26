package com.kylecorry.trail_sense.shared.sensors.altimeter

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class CachingAltimeterWrapper(context: Context, override val altimeter: IAltimeter) :
    AbstractSensor(),
    AltimeterWrapper {

    private val cache = PreferencesSubsystem.getInstance(context).preferences

    override val altitudeAccuracy: Float?
        get() = if (altimeter is AltimeterWrapper) altimeter.altitudeAccuracy else null

    override val altitude: Float
        get() = altimeter.altitude

    override val hasValidReading: Boolean
        get() = altimeter.hasValidReading

    override val quality: Quality
        get() = altimeter.quality

    override fun startImpl() {
        altimeter.start(this::onReading)
    }

    override fun stopImpl() {
        altimeter.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        cache.putFloat(LAST_ALTITUDE_KEY, altitude)
        notifyListeners()
        return true
    }

    companion object {
        const val LAST_ALTITUDE_KEY = "last_altitude_2"
    }
}
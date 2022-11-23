package com.kylecorry.trail_sense.shared.sensors.altimeter

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.UserPreferences

class CachedAltimeter(context: Context, private val updateFrequency: Long = 20L) : AbstractSensor(),
    IAltimeter {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = true

    private val cache by lazy { Preferences(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Timer { notifyListeners() }

    override val altitude: Float
        get() = cache.getFloat(CachingAltimeterWrapper.LAST_ALTITUDE_KEY)
            ?: userPrefs.altitudeOverride

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}
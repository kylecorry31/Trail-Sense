package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class CachedAltimeter(context: Context, private val updateFrequency: Long = 20L): AbstractSensor(),
    IAltimeter {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = true

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Intervalometer { notifyListeners() }

    override val altitude: Float
        get() = prefs.getFloat(GPS.LAST_ALTITUDE, userPrefs.altitudeOverride)

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}
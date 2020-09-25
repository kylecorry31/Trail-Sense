package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class CachedGPS(context: Context, private val updateFrequency: Long = 20L) : AbstractSensor(),
    IGPS {
    override val location: Coordinate
        get() {
            // TODO: Losing some precision here
            val lat =
                prefs.getFloat(GPS.LAST_LATITUDE, userPrefs.locationOverride.latitude.toFloat())
            val lng =
                prefs.getFloat(GPS.LAST_LONGITUDE, userPrefs.locationOverride.longitude.toFloat())
            return Coordinate(lat.toDouble(), lng.toDouble())
        }
    override val speed: Float
        get() = prefs.getFloat(GPS.LAST_SPEED, 0.0f)
    override val verticalAccuracy: Float?
        get() = null
    override val horizontalAccuracy: Float?
        get() = null
    override val satellites: Int
        get() = 0
    override val hasValidReading: Boolean
        get() = true
    override val altitude: Float
        get() = prefs.getFloat(GPS.LAST_ALTITUDE, userPrefs.altitudeOverride)

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Intervalometer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}
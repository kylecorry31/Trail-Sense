package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.Speed
import com.kylecorry.trailsensecore.domain.units.TimeUnits
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.andromeda.core.time.Timer
import java.time.Instant

class CachedGPS(context: Context, private val updateFrequency: Long = 20L) : AbstractSensor(),
    IGPS {
    override val location: Coordinate
        get() {
            val lat = cache.getDouble(CustomGPS.LAST_LATITUDE) ?: userPrefs.locationOverride.latitude
            val lng = cache.getDouble(CustomGPS.LAST_LONGITUDE) ?: userPrefs.locationOverride.longitude
            return Coordinate(lat, lng)
        }
    override val speed: Speed
        get() = Speed(cache.getFloat(CustomGPS.LAST_SPEED) ?: 0.0f, DistanceUnits.Meters, TimeUnits.Seconds)
    override val time: Instant
        get() = Instant.now()
    override val verticalAccuracy: Float?
        get() = null
    override val horizontalAccuracy: Float?
        get() = null
    override val satellites: Int
        get() = 0
    override val hasValidReading: Boolean
        get() = true
    override val altitude: Float
        get() = cache.getFloat(CustomGPS.LAST_ALTITUDE) ?: userPrefs.altitudeOverride
    override val mslAltitude: Float?
        get() = altitude

    private val cache by lazy { Preferences(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Timer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}
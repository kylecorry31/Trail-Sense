package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Instant

class CachedGPS(context: Context, private val updateFrequency: Long = 20L) : AbstractSensor(),
    IGPS {
    override val location: Coordinate
        get() {
            val lat = cache.getDouble(CustomGPS.LAST_LATITUDE) ?: userPrefs.locationOverride.latitude
            val lng = cache.getDouble(CustomGPS.LAST_LONGITUDE) ?: userPrefs.locationOverride.longitude
            return Coordinate(lat, lng)
        }
    override val speed: Float
        get() = cache.getFloat(CustomGPS.LAST_SPEED) ?: 0.0f
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

    private val cache by lazy { Cache(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = Intervalometer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}
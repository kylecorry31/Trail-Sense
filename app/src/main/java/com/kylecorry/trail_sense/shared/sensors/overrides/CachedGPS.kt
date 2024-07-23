package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import java.time.Instant

class CachedGPS(context: Context, private val updateFrequency: Long = 20L) : AbstractSensor(),
    ISatelliteGPS {
    override val location: Coordinate
        get() {
            val lat =
                cache.getDouble(CustomGPS.LAST_LATITUDE) ?: userPrefs.locationOverride.latitude
            val lng =
                cache.getDouble(CustomGPS.LAST_LONGITUDE) ?: userPrefs.locationOverride.longitude
            return Coordinate(lat, lng)
        }
    override val speed: Speed
        get() = Speed(
            cache.getFloat(CustomGPS.LAST_SPEED) ?: 0.0f,
            DistanceUnits.Meters,
            TimeUnits.Seconds
        )
    override val speedAccuracy: Float?
        get() = null
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
    override val bearing: Bearing?
        get() = null
    override val bearingAccuracy: Float?
        get() = null
    override val fixTimeElapsedNanos: Long?
        get() = null
    override val mslAltitude: Float
        get() = altitude
    override val rawBearing: Float?
        get() = null
    override val satelliteDetails: List<Satellite>?
        get() = null

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val userPrefs by lazy { UserPreferences(context) }
    private val intervalometer = CoroutineTimer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}
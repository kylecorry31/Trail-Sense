package com.kylecorry.trail_sense.shared.sensors.overrides

import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.InactiveGPS
import com.kylecorry.trail_sense.shared.sensors.gps.MockedGPS
import java.time.Instant

class CachedGPS(context: Context, private val updateFrequency: Long = 1000L) : AbstractSensor(),
    ISatelliteGPS, InactiveGPS, MockedGPS {
    override val location: Coordinate
        get() {
            val cached = cached
            val override = userPrefs.gps.locationOverride
            return Coordinate(
                cached?.latitude ?: override.latitude,
                cached?.longitude ?: override.longitude
            )
        }
    override val speed: Speed
        get() = Speed.from(
            cached?.speed ?: 0.0f,
            DistanceUnits.Meters,
            TimeUnits.Seconds
        )
    override val speedAccuracy: Float?
        get() = null
    override val time: Instant
        get() = Instant.now()
    override val verticalAccuracy: Float?
        get() = cached?.verticalAccuracy
    override val horizontalAccuracy: Float?
        get() = cached?.horizontalAccuracy
    override val satellites: Int
        get() = 0
    override val hasValidReading: Boolean
        get() = true
    override val altitude: Float
        get() = cached?.altitude ?: userPrefs.altitudeOverride
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
    private val cached
        get() = CacheGPSModule.getCachedData(cache)
    private val intervalometer = CoroutineTimer { notifyListeners() }

    override fun startImpl() {
        intervalometer.interval(updateFrequency)
    }

    override fun stopImpl() {
        intervalometer.stop()
    }
}

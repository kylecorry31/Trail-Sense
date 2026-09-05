package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.BadReadingRejectionGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.KalmanGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.TimeoutGPSModule
import java.time.Duration
import java.time.Instant


class CustomGPS(
    private val context: Context,
    private val gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
) : AbstractSensor(), ISatelliteGPS {

    override val hasValidReading: Boolean
        get() = hadRecentValidReading()

    override val satellites: Int?
        get() = data.satellites

    override val quality: Quality
        get() = data.quality
    override val rawBearing: Float?
        get() = data.rawBearing
    override val satelliteDetails: List<Satellite>?
        get() = data.satelliteDetails

    override val horizontalAccuracy: Float?
        get() = data.horizontalAccuracy

    override val verticalAccuracy: Float?
        get() = data.verticalAccuracy

    override val location: Coordinate
        get() {
            if (cacheModule.hasNewerReading(data)) {
                cacheModule.restore(data)
            }
            return data.location
        }

    override val speed: Speed
        get() = data.speed
    override val speedAccuracy: Float?
        get() = data.speedAccuracy

    override val time: Instant
        get() = data.time

    override val altitude: Float
        get() = data.altitude
    override val bearing: Bearing?
        get() = data.bearing
    override val bearingAccuracy: Float?
        get() = data.bearingAccuracy

    override val fixTimeElapsedNanos: Long?
        get() = data.fixTimeElapsedNanos

    override val mslAltitude: Float?
        get() = data.mslAltitude

    val isTimedOut: Boolean
        get() = timeoutModule.isTimedOut

    private val baseGPS: ISatelliteGPS by lazy {
        GPS(context.applicationContext, frequency = gpsFrequency)
    }
    private val userPrefs by lazy { UserPreferences(context) }

    // The last accepted reading and the reading being evaluated
    private val data = ModularGPSData()
    private val candidate = ModularGPSData()

    private val cacheModule = CacheGPSModule()
    private val timeoutModule = TimeoutGPSModule(this::tryUpdateLocation, this::notifyListeners)

    // The cache runs last so it records the reading as the other modules leave it
    private val modules = listOfNotNull(
        BadReadingRejectionGPSModule(),
        MeanSeaLevelGPSModule(),
        SpeedGPSModule(),
        timeoutModule,
        if (userPrefs.useFilteredGPS) KalmanGPSModule() else null,
        cacheModule
    )

    private var hadValidReading = false

    init {
        cacheModule.restore(data)

        if (baseGPS.hasValidReading) {
            tryUpdateLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!GPS.isAvailable(context)) {
            return
        }

        // If this is being restarted, reload the value from cache if there's a newer reading there
        if (hadValidReading && cacheModule.hasNewerReading(data)) {
            cacheModule.restore(data)
            notifyListeners()
        }

        baseGPS.start(this::onLocationUpdate)
        modules.forEach { it.start(data) }
    }

    override fun stopImpl() {
        baseGPS.stop(this::onLocationUpdate)
        modules.forEach { it.stop(data) }
    }

    private fun onLocationUpdate(): Boolean {
        val shouldNotify = tryUpdateLocation()
        if (shouldNotify) {
            notifyListeners()
        }
        return true
    }

    @Synchronized
    private fun tryUpdateLocation(): Boolean {
        candidate.populateFromGPS(baseGPS)

        // Determine if the new location should be used, if not, keep the old location
        if (modules.any { !it.update(data, candidate) }) {
            return false
        }

        // This can happen when the cache is restored with the same reading as the base GPS or a secondary field updates
        val isSameReading = candidate.time == data.time

        candidate.copyInto(data)

        return if (data.location != Coordinate.zero) {
            hadValidReading = true
            !isSameReading
        } else {
            false
        }
    }

    private fun hadRecentValidReading(): Boolean {
        val last = time
        val now = Instant.now()
        return Duration.between(last, now) <= RECENT_READING_THRESHOLD &&
                location != Coordinate.zero
    }

    companion object {
        private val RECENT_READING_THRESHOLD: Duration = Duration.ofMinutes(2)
    }
}

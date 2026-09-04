package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.time.Time.isInPast
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.BadReadingRejectionGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.FusedGPS
import com.kylecorry.trail_sense.shared.sensors.gps.MeanSeaLevelGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedGPSModule
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger


class CustomGPS(
    private val context: Context,
    private val gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    private val updateFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
) : AbstractSensor(), ISatelliteGPS {

    private val logger = getAppService<Logger>()

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
            if (cacheHasNewerReading()) {
                updateFromCache()
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
        get() = _isTimedOut

    private val baseGPS: ISatelliteGPS by lazy {
        if (userPrefs.useFilteredGPS) {
            FusedGPS(
                GPS(context.applicationContext, frequency = gpsFrequency),
                updateFrequency
            )
        } else {
            GPS(context.applicationContext, frequency = gpsFrequency)
        }
    }
    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val userPrefs by lazy { UserPreferences(context) }

    private val timeout = CoroutineTimer {
        onTimeout()
    }

    // The last accepted reading and the reading being evaluated
    private val data = ModularGPSData()
    private val candidate = ModularGPSData()

    private val modules = listOf(
        BadReadingRejectionGPSModule(),
        MeanSeaLevelGPSModule(),
        SpeedGPSModule()
    )

    private var _isTimedOut = false

    private var hadValidReading = false

    private val diagnosticId = nextDiagnosticId.getAndIncrement()

    @Volatile
    private var isStarted = false

    init {
        updateFromCache()

        if (baseGPS.hasValidReading) {
            tryUpdateLocation()
        }
    }

    private fun cacheHasNewerReading(): Boolean {
        val cacheTime = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        return cacheTime > data.time && cacheTime.isInPast()
    }

    private fun updateFromCache() {
        data.location = Coordinate(
            cache.getDouble(LAST_LATITUDE) ?: 0.0,
            cache.getDouble(LAST_LONGITUDE) ?: 0.0
        )
        data.altitude = cache.getFloat(LAST_ALTITUDE) ?: 0f
        data.speed =
            Speed.from(cache.getFloat(LAST_SPEED) ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        data.time = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        data.horizontalAccuracy = cache.getFloat(LAST_HORIZONTAL_ACCURACY)
        data.verticalAccuracy = cache.getFloat(LAST_VERTICAL_ACCURACY)
        data.quality = Quality.Unknown
        data.satellites = null
        data.satelliteDetails = null
        data.mslAltitude = null
        data.rawBearing = null
        data.bearing = null
        data.bearingAccuracy = null
        data.speedAccuracy = null
        data.fixTimeElapsedNanos = null
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!GPS.isAvailable(context)) {
            return
        }

        isStarted = true

        // If this is being restarted, reload the value from cache if there's a newer reading there
        if (hadValidReading && cacheHasNewerReading()) {
            updateFromCache()
            notifyListeners()
        }

        baseGPS.start(this::onLocationUpdate)
        timeout.once(TIMEOUT_DURATION)
        modules.forEach { it.start(data) }
    }

    override fun stopImpl() {
        isStarted = false
        baseGPS.stop(this::onLocationUpdate)
        timeout.stop()
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

        // Reset the timeout, there's a valid reading
        if (isStarted) {
            timeout.once(TIMEOUT_DURATION)
        }
        _isTimedOut = false

        candidate.copyInto(data)
        updateCache()

        return if (data.location != Coordinate.zero) {
            hadValidReading = true
            !isSameReading
        } else {
            false
        }
    }

    private fun updateCache() {
        cache.putFloat(LAST_ALTITUDE, altitude)
        cache.putLong(LAST_UPDATE, time.toEpochMilli())
        cache.putFloat(LAST_SPEED, speed.value)
        cache.putDouble(LAST_LONGITUDE, location.longitude)
        cache.putDouble(LAST_LATITUDE, location.latitude)
        val currentHorizontalAccuracy = horizontalAccuracy
        if (currentHorizontalAccuracy != null) {
            cache.putFloat(LAST_HORIZONTAL_ACCURACY, currentHorizontalAccuracy)
        } else {
            cache.remove(LAST_HORIZONTAL_ACCURACY)
        }
        val currentVerticalAccuracy = verticalAccuracy
        if (currentVerticalAccuracy != null) {
            cache.putFloat(LAST_VERTICAL_ACCURACY, currentVerticalAccuracy)
        } else {
            cache.remove(LAST_VERTICAL_ACCURACY)
        }
    }

    private fun onTimeout() {
        if (!isStarted) {
            return
        }

        logger.debug(TAG, "[$diagnosticId] Timed out after ${TIMEOUT_DURATION.seconds}s")

        if (!tryUpdateLocation()) {
            logger.debug(
                TAG,
                "[$diagnosticId] No valid reading to update to, keeping a reading from " +
                        "${Duration.between(data.time, Instant.now()).toMillis()}ms ago"
            )
            _isTimedOut = true
            timeout.once(TIMEOUT_DURATION)
        }

        notifyListeners()
    }

    private fun hadRecentValidReading(): Boolean {
        val last = time
        val now = Instant.now()
        return Duration.between(last, now) <= RECENT_READING_THRESHOLD &&
                location != Coordinate.zero
    }

    companion object {
        const val LAST_LATITUDE = "last_latitude_double"
        const val LAST_LONGITUDE = "last_longitude_double"
        const val LAST_ALTITUDE = "last_altitude"
        const val LAST_SPEED = "last_speed"
        const val LAST_UPDATE = "last_update"
        const val LAST_HORIZONTAL_ACCURACY = "last_horizontal_accuracy"
        const val LAST_VERTICAL_ACCURACY = "last_vertical_accuracy"

        private val TIMEOUT_DURATION = Duration.ofSeconds(10)
        private val RECENT_READING_THRESHOLD: Duration = Duration.ofMinutes(2)
        private const val TAG = "CustomGPS"

        // This is used to distinguish instances of this class in the logs, since there can be multiple instances of this class at once
        private val nextDiagnosticId = AtomicInteger(1)

        fun clearCache() {
            val cache = getAppService<PreferencesSubsystem>().preferences
            cache.remove(LAST_ALTITUDE)
            cache.remove(LAST_UPDATE)
            cache.remove(LAST_SPEED)
            cache.remove(LAST_LONGITUDE)
            cache.remove(LAST_LATITUDE)
            cache.remove(LAST_HORIZONTAL_ACCURACY)
            cache.remove(LAST_VERTICAL_ACCURACY)
        }

    }
}

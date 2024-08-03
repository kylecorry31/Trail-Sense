package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.time.Time.isInPast
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.FusedGPS
import com.kylecorry.trail_sense.shared.sensors.speedometer.SpeedEstimator
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant


class CustomGPS(
    private val context: Context,
    private val gpsFrequency: Duration = Duration.ofMillis(20),
    private val updateFrequency: Duration = Duration.ofMillis(20),
) : AbstractSensor(), ISatelliteGPS {

    override val hasValidReading: Boolean
        get() = hadRecentValidReading()

    override val satellites: Int?
        get() = _satellites

    override val quality: Quality
        get() = _quality
    override val rawBearing: Float?
        get() = _rawBearing
    override var satelliteDetails: List<Satellite>? = null
        private set

    override val horizontalAccuracy: Float?
        get() = _horizontalAccuracy

    override val verticalAccuracy: Float?
        get() = _verticalAccuracy

    override val location: Coordinate
        get() {
            if (cacheHasNewerReading()) {
                updateFromCache()
            }
            return _location
        }

    override val speed: Speed
        get() = _speed
    override val speedAccuracy: Float?
        get() = _speedAccuracy

    override val time: Instant
        get() = _time

    override val altitude: Float
        get() = _altitude
    override val bearing: Bearing?
        get() = _bearing
    override val bearingAccuracy: Float?
        get() = _bearingAccuracy

    override var fixTimeElapsedNanos: Long? = null
        private set

    override val mslAltitude: Float?
        get() = _mslAltitude

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

    private val geoidTimer = CoroutineTimer {
        geoidOffset = AltitudeCorrection.getGeoid(context, location)
    }

    private var _altitude = 0f
    private var _time = Instant.now()
    private var _quality = Quality.Unknown
    private var _horizontalAccuracy: Float? = null
    private var _verticalAccuracy: Float? = null
    private var _satellites: Int? = null
    private var _speed: Speed = Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    private var _location = Coordinate.zero
    private var _mslAltitude: Float? = null
    private var _isTimedOut = false
    private var mslOffset = 0f
    private var geoidOffset = 0f
    private var _rawBearing: Float? = null
    private var _bearing: Bearing? = null
    private var _bearingAccuracy: Float? = null
    private var _speedAccuracy: Float? = null

    private val locationHistory = RingBuffer<Pair<ApproximateCoordinate, Instant>>(10)

    init {
        if (baseGPS.hasValidReading) {
            updateFromBase()
        } else {
            updateFromCache()
        }
    }

    private fun updateFromBase() {
        _location = baseGPS.location
        _speed = baseGPS.speed
        _verticalAccuracy = baseGPS.verticalAccuracy
        _time = baseGPS.time
        fixTimeElapsedNanos = baseGPS.fixTimeElapsedNanos
        _horizontalAccuracy = baseGPS.horizontalAccuracy
        _quality = baseGPS.quality
        _satellites = baseGPS.satellites
        satelliteDetails = baseGPS.satelliteDetails
        _mslAltitude = baseGPS.mslAltitude
        val newMSLOffset = baseGPS.altitude - (baseGPS.mslAltitude ?: baseGPS.altitude)
        if (newMSLOffset != 0f) {
            mslOffset = newMSLOffset
        }

        _altitude = baseGPS.altitude - getGeoidOffset(_location)

        _rawBearing = baseGPS.rawBearing
        _bearing = baseGPS.bearing
        _bearingAccuracy = baseGPS.bearingAccuracy
        _speedAccuracy = baseGPS.speedAccuracy


        updateSpeed()

        updateCache()
    }

    private fun updateSpeed() {
        val locations = locationHistory.toList()

        val oldestLocation = locations.firstOrNull()

        // If the speed is zero, estimate the speed
        if (_speed.speed == 0f && oldestLocation != null) {
            val currentLocation = ApproximateCoordinate.from(
                _location,
                Distance.meters(_horizontalAccuracy?.real(10f) ?: 10f)
            )

            _speed = SpeedEstimator.calculate(
                oldestLocation.first,
                currentLocation,
                oldestLocation.second,
                _time
            )
        }

        // Add to location history every second
        if (locations.isEmpty() || Duration.between(locations.last().second, _time).seconds >= 1) {
            locationHistory.add(
                ApproximateCoordinate.from(
                    _location,
                    Distance.meters(_horizontalAccuracy?.real(10f) ?: 10f)
                ) to _time
            )
        }
    }

    private fun getGeoidOffset(location: Coordinate): Float {
        if (userPrefs.useNMEA && mslOffset != 0f) {
            return mslOffset
        }

        if (geoidOffset != 0f) {
            return geoidOffset
        }

        // This is not ideal, but an offset is needed (and this service caches it)
        geoidOffset = runBlocking { AltitudeCorrection.getGeoid(context, location) }
        return geoidOffset
    }

    private fun cacheHasNewerReading(): Boolean {
        val cacheTime = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        return cacheTime > time && cacheTime.isInPast()
    }

    private fun updateFromCache() {
        _location = Coordinate(
            cache.getDouble(LAST_LATITUDE) ?: 0.0,
            cache.getDouble(LAST_LONGITUDE) ?: 0.0
        )
        _altitude = cache.getFloat(LAST_ALTITUDE) ?: 0f
        _speed = Speed(cache.getFloat(LAST_SPEED) ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        _time = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!GPS.isAvailable(context)) {
            return
        }

        baseGPS.start(this::onLocationUpdate)
        timeout.once(TIMEOUT_DURATION)
        geoidTimer.interval(Duration.ofMillis(200))
    }

    override fun stopImpl() {
        baseGPS.stop(this::onLocationUpdate)
        timeout.stop()
        geoidTimer.stop()
    }

    private fun onLocationUpdate(): Boolean {
        if (!baseGPS.hasValidReading) {
            return true
        }

        // Determine if the new location should be used, if not, return the old location
        if (!shouldUpdateReading()) {
            // Reset the timeout, there's a valid reading
            timeout.once(TIMEOUT_DURATION)
            _isTimedOut = false
            notifyListeners()
            return true
        }

        var shouldNotify = true

        // Verify satellite requirement for notification
        // If satellite count is null, then the phone doesn't support satellite count
        val satelliteCount = baseGPS.satellites
        val hasFix = satelliteCount == null || !userPrefs.requiresSatellites || satelliteCount >= 4
        if (!hasFix) {
            shouldNotify = false
        } else {
            // Reset the timeout, there's a valid reading
            timeout.once(TIMEOUT_DURATION)
            _isTimedOut = false
        }

        updateFromBase()

        if (shouldNotify && location != Coordinate.zero) {
            notifyListeners()
        }

        return true
    }

    private fun updateCache() {
        cache.putFloat(LAST_ALTITUDE, altitude)
        cache.putLong(LAST_UPDATE, time.toEpochMilli())
        cache.putFloat(LAST_SPEED, speed.speed)
        cache.putDouble(LAST_LONGITUDE, location.longitude)
        cache.putDouble(LAST_LATITUDE, location.latitude)
    }

    private fun onTimeout() {
        _isTimedOut = true
        notifyListeners()
        timeout.once(TIMEOUT_DURATION)
    }

    private fun hadRecentValidReading(): Boolean {
        val last = time
        val now = Instant.now()
        val recentThreshold = Duration.ofMinutes(2)
        return Duration.between(last, now) <= recentThreshold && location != Coordinate.zero
    }

    private fun shouldUpdateReading(): Boolean {
        // Modified from https://stackoverflow.com/questions/10588982/retrieving-of-satellites-used-in-gps-fix-from-android
        if (location == Coordinate.zero) {
            return true
        }

        val timeDelta = Duration.between(time, baseGPS.time)
        val isSignificantlyNewer: Boolean = timeDelta > Duration.ofMinutes(2)
        val isSignificantlyOlder: Boolean = timeDelta < Duration.ofMinutes(-2)
        val isNewer = timeDelta > Duration.ZERO

        val isLastTimeInFuture = time.isAfter(Instant.now().plusMillis(500))
        if (isLastTimeInFuture) {
            return true
        }

        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }

        val accuracyDelta = (baseGPS.horizontalAccuracy ?: 0f) - (horizontalAccuracy ?: 0f)
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 30

        if (isMoreAccurate) {
            return true
        }

        return isNewer && !isSignificantlyLessAccurate
    }

    companion object {
        const val LAST_LATITUDE = "last_latitude_double"
        const val LAST_LONGITUDE = "last_longitude_double"
        const val LAST_ALTITUDE = "last_altitude"
        const val LAST_SPEED = "last_speed"
        const val LAST_UPDATE = "last_update"
        private val TIMEOUT_DURATION = Duration.ofSeconds(10)
    }
}
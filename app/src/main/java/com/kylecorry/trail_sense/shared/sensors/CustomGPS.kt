package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.math.RingBuffer
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.time.Time.isInPast
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.FusedGPS
import com.kylecorry.trail_sense.shared.sensors.speedometer.SpeedEstimator
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger


class CustomGPS(
    private val context: Context,
    private val gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    private val updateFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
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
        get() = _bearing?.let { Bearing.from(it) }
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

    private val geoidRunner = CoroutineQueueRunner()
    private val geoidTask = BackgroundTask {
        geoidRunner.enqueue {
            val currentLocation = location
            geoidOffset = AltitudeCorrection.getGeoid(currentLocation)
            geoidLocation = currentLocation
        }
    }

    private var _altitude = 0f
    private var _time = Instant.now()
    private var _quality = Quality.Unknown
    private var _horizontalAccuracy: Float? = null
    private var _verticalAccuracy: Float? = null
    private var _satellites: Int? = null
    private var _speed: Speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    private var _location = Coordinate.zero
    private var _mslAltitude: Float? = null
    private var _isTimedOut = false
    private var mslOffset = 0f

    @Volatile
    private var geoidOffset = 0f

    @Volatile
    private var geoidLocation: Coordinate? = null
    private var _rawBearing: Float? = null
    private var _bearing: Float? = null
    private var _bearingAccuracy: Float? = null
    private var _speedAccuracy: Float? = null

    private var hadValidReading = false

    private val diagnosticId = nextDiagnosticId.getAndIncrement()

    @Volatile
    private var isStarted = false

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
        _bearing = baseGPS.bearing?.value
        _bearingAccuracy = baseGPS.bearingAccuracy
        _speedAccuracy = baseGPS.speedAccuracy


        updateSpeed()

        updateCache()
    }

    private fun updateSpeed() {
        val locations = locationHistory.toList()

        val oldestLocation = locations.firstOrNull()

        // If the speed is zero, estimate the speed
        if (_speed.value == 0f && oldestLocation != null) {
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

        val lastLocation = geoidLocation

        if (lastLocation == null) {
            // This is not ideal, but an offset is needed (and this service caches it)
            geoidOffset = runBlocking { AltitudeCorrection.getGeoid(location) }
            geoidLocation = location
        } else if (!AltitudeCorrection.isSameGeoid(lastLocation, location)) {
            geoidTask.start()
        }

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
        _speed =
            Speed.from(cache.getFloat(LAST_SPEED) ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        _time = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        _horizontalAccuracy = cache.getFloat(LAST_HORIZONTAL_ACCURACY)
        _verticalAccuracy = cache.getFloat(LAST_VERTICAL_ACCURACY)
        _quality = Quality.Unknown
        _satellites = null
        satelliteDetails = null
        _mslAltitude = null
        _rawBearing = null
        _bearing = null
        _bearingAccuracy = null
        _speedAccuracy = null
        fixTimeElapsedNanos = null
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
        // Load the offset for the last known location so the first fix doesn't have to wait on it
        geoidTask.start()
    }

    override fun stopImpl() {
        isStarted = false
        baseGPS.stop(this::onLocationUpdate)
        timeout.stop()
        geoidTask.stop()
        geoidRunner.cancel()
    }

    private fun onLocationUpdate(): Boolean {
        if (!baseGPS.hasValidReading) {
            return true
        }

        // Determine if the new location should be used, if not, return the old location
        if (!shouldAcceptNewReading()) {
            // Reset the timeout, there's a valid reading
            timeout.once(TIMEOUT_DURATION)
            if (_isTimedOut) {
                _isTimedOut = false
                notifyListeners()
            }
            return true
        }

        var shouldNotify = true

        // Verify satellite requirement for notification
        // If satellite count is null, then the phone doesn't support satellite count
        val satelliteCount = baseGPS.satellites
        val hasFix = satelliteCount == null || !userPrefs.requiresSatellites || satelliteCount >= 4
        if (!hasFix) {
            Log.d(
                TAG,
                "[$diagnosticId] Bad Location Fix: $satelliteCount satellites, ${baseGPS.horizontalAccuracy}m accuracy"
            )
            shouldNotify = false
        } else {
            // Reset the timeout, there's a valid reading
            timeout.once(TIMEOUT_DURATION)
            _isTimedOut = false
        }

        updateFromBase()

        if (shouldNotify && location != Coordinate.zero) {
            hadValidReading = true
            notifyListeners()
        }

        return true
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

        _isTimedOut = true
        Log.d(
            TAG,
            "[$diagnosticId] Timed out after ${TIMEOUT_DURATION.seconds}s, keeping a reading from " +
                    "${Duration.between(_time, Instant.now()).toMillis()}ms ago"
        )
        notifyListeners()
        timeout.once(TIMEOUT_DURATION)
    }

    private fun hadRecentValidReading(): Boolean {
        val last = time
        val now = Instant.now()
        return Duration.between(last, now) <= RECENT_READING_THRESHOLD &&
                location != Coordinate.zero
    }

    private fun shouldAcceptNewReading(): Boolean {
        if (!userPrefs.filterLocationReadings) {
            return true
        }

        if (_location == Coordinate.zero) {
            return true
        }

        // The current reading is somehow in the future, so just accept a new reading (prevents stuck readings)
        val isLastTimeInFuture = _time.isAfter(Instant.now().plusMillis(500))
        if (isLastTimeInFuture) {
            return true
        }

        // The new reading isn't newer, so reject it
        val timeDelta = Duration.between(_time, baseGPS.time)
        if (timeDelta <= Duration.ZERO) {
            logRejectedReading("not newer")
            return false
        }

        // A reading hasn't been accepted in a while, so accept this one even if it's an "outlier"
        if (timeDelta > NEW_READING_DURATION) {
            return true
        }

        // If the GPS doesn't report accuracy, just take the new reading
        val newAccuracy = baseGPS.horizontalAccuracy
        if (newAccuracy == null || Arithmetic.isZero(newAccuracy)) {
            return true
        }

        // The new reading is too inaccurate to be useful, unless it still improves on the current one
        val currentAccuracy = _horizontalAccuracy?.takeIf { it > 0f } ?: DEFAULT_ACCURACY
        if (newAccuracy > maxOf(MAX_ACCEPTABLE_ACCURACY, currentAccuracy)) {
            logRejectedReading("poor accuracy")
            return false
        }

        // The new reading is farther away than the user could have traveled since the last one
        val seconds = timeDelta.toMillis() / 1000f
        val distance = _location.distanceTo(baseGPS.location)
        val estimatedSpeed = _speed.value.real(0f)
            .coerceIn(MIN_SPEED_ALLOWANCE, MAX_SPEED_ALLOWANCE)

        val maxDistance = (estimatedSpeed * seconds + currentAccuracy + newAccuracy) * MAX_DISTANCE_FACTOR
        if (distance > maxDistance) {
            logRejectedReading("implausible movement (max allowed: ${maxDistance}m)")
            return false
        }

        return true
    }

    private fun logRejectedReading(reason: String) {
        Log.d(
            TAG, "[$diagnosticId] Location Rejected: $reason, Time Delta: ${
                Duration.between(time, baseGPS.time).toMillis()
            }ms, Distance: ${_location.distanceTo(baseGPS.location)}, Accuracy: ${_horizontalAccuracy}m -> ${baseGPS.horizontalAccuracy}m, Age: ${
                Duration.between(
                    _time,
                    Instant.now()
                ).toMillis()
            }ms"
        )
    }

    companion object {
        const val LAST_LATITUDE = "last_latitude_double"
        const val LAST_LONGITUDE = "last_longitude_double"
        const val LAST_ALTITUDE = "last_altitude"
        const val LAST_SPEED = "last_speed"
        const val LAST_UPDATE = "last_update"
        const val LAST_HORIZONTAL_ACCURACY = "last_horizontal_accuracy"
        const val LAST_VERTICAL_ACCURACY = "last_vertical_accuracy"

        // The min and max speed for location filtering (m/s)
        private const val MIN_SPEED_ALLOWANCE = 1f
        private const val MAX_SPEED_ALLOWANCE = 50f
        // A factor to scale the max distance of new readings
        private const val MAX_DISTANCE_FACTOR = 1.1f
        private const val DEFAULT_ACCURACY = 30f

        // Readings with this accuracy are too poor to accept, wait for another reading
        private const val MAX_ACCEPTABLE_ACCURACY = 50f
        private val TIMEOUT_DURATION = Duration.ofSeconds(10)

        // How often to force a new reading even if the accuracy is worse
        private val NEW_READING_DURATION = Duration.ofSeconds(10)
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

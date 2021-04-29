package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.isInPast
import com.kylecorry.trailsensecore.domain.geo.ApproximateCoordinate
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.units.*
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.GPS
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import java.time.Duration
import java.time.Instant


class CustomGPS(private val context: Context) : AbstractSensor(), IGPS {

    private val odometer by lazy { SensorService(context).getOdometer() }

    override val hasValidReading: Boolean
        get() = hadRecentValidReading()

    override val satellites: Int
        get() = _satellites

    override val quality: Quality
        get() = _quality

    override val horizontalAccuracy: Float?
        get() = _horizontalAccuracy

    override val verticalAccuracy: Float?
        get() = _verticalAccuracy

    override val location: Coordinate
        get(){
            if (cacheHasNewerReading()){
                updateFromCache()
            }
            return _location
        }

    override val speed: Speed
        get() = _speed

    override val time: Instant
        get() = _time

    override val altitude: Float
        get() = _altitude

    override val mslAltitude: Float?
        get() = _mslAltitude

    val isTimedOut: Boolean
        get() = _isTimedOut

    private val baseGPS by lazy { GPS(context.applicationContext) }
    private val cache by lazy { Cache(context.applicationContext) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val sensorChecker by lazy { SensorChecker(context) }

    private val timeout = Intervalometer {
        onTimeout()
    }

    private var _altitude = 0f
    private var _time = Instant.now()
    private var _quality = Quality.Unknown
    private var _horizontalAccuracy: Float? = null
    private var _verticalAccuracy: Float? = null
    private var _satellites: Int = 0
    private var _speed: Speed = Speed(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    private var _location = Coordinate.zero
    private var _mslAltitude: Float? = null
    private var _isTimedOut = false
    private var mslOffset = 0f

    init {
        if (baseGPS.hasValidReading){
            updateFromBase()
        } else {
            updateFromCache()
        }
    }

    private fun updateFromBase(){
        _location = baseGPS.location
        _speed = baseGPS.speed
        _verticalAccuracy = baseGPS.verticalAccuracy
        _altitude = baseGPS.altitude
        _time = baseGPS.time
        _horizontalAccuracy = baseGPS.horizontalAccuracy
        _quality = baseGPS.quality
        _satellites = baseGPS.satellites
        _mslAltitude = baseGPS.mslAltitude

        updateCache()

        if (userPrefs.useAltitudeOffsets) {
            _altitude -= AltitudeCorrection.getOffset(_location, context)
        }
    }

    private fun cacheHasNewerReading(): Boolean {
        val cacheTime = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        return cacheTime > time && cacheTime.isInPast()
    }

    private fun updateFromCache(){
        _location = Coordinate(
            cache.getDouble(LAST_LATITUDE) ?: 0.0,
            cache.getDouble(LAST_LONGITUDE) ?: 0.0
        )
        _altitude = cache.getFloat(LAST_ALTITUDE) ?: 0f
        _speed = Speed(cache.getFloat(LAST_SPEED) ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        _time = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        if (userPrefs.useAltitudeOffsets) {
            _altitude -= AltitudeCorrection.getOffset(_location, context)
        }
    }

    private fun updateOdometer(){
        if (userPrefs.usePedometer){
            return
        }

        odometer.addLocation(ApproximateCoordinate.from(location, Distance.meters(_horizontalAccuracy ?: 0f)))
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!sensorChecker.hasGPS()) {
            return
        }

        baseGPS.start(this::onLocationUpdate)
        timeout.once(TIMEOUT_DURATION)
    }

    override fun stopImpl() {
        baseGPS.stop(this::onLocationUpdate)
        timeout.stop()
    }

    private fun onLocationUpdate(): Boolean {
        if (!baseGPS.hasValidReading){
            return true
        }

        // Determine if the new location should be used, if not, return the old location
        if (!shouldUpdateReading()){
            // Reset the timeout, there's a valid reading
            timeout.once(TIMEOUT_DURATION)
            _isTimedOut = false
            updateOdometer()
            notifyListeners()
            return true
        }

        var shouldNotify = true

        // Verify satellite requirement for notification
        if (userPrefs.requiresSatellites && baseGPS.satellites < 4){
            shouldNotify = false
        } else {
            // Reset the timeout, there's a valid reading
            timeout.once(TIMEOUT_DURATION)
            _isTimedOut = false
        }

        updateFromBase()

        if (userPrefs.useAltitudeOffsets && _mslAltitude != null){
            _altitude = _mslAltitude ?: 0f
        }

        if (shouldNotify && location != Coordinate.zero){
            updateOdometer()
            notifyListeners()
        }

        return true
    }

    private fun updateCache(){
        cache.putFloat(LAST_ALTITUDE, altitude)
        cache.putLong(LAST_UPDATE, time.toEpochMilli())
        cache.putFloat(LAST_SPEED, speed.speed)
        cache.putDouble(LAST_LONGITUDE, location.longitude)
        cache.putDouble(LAST_LATITUDE, location.latitude)
    }

    private fun onTimeout(){
        _isTimedOut = true
        updateOdometer()
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
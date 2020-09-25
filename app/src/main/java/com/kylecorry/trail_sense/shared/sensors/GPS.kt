package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.Accuracy
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.AbstractSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS


class GPS(private val context: Context) : AbstractSensor(), IGPS {

    override val hasValidReading: Boolean
        get() = hadRecentValidReading()

    override val satellites: Int
        get() = _satellites

    override val accuracy: Accuracy
        get() = _accuracy

    override val horizontalAccuracy: Float?
        get() = _horizontalAccuracy

    override val verticalAccuracy: Float?
        get() = _verticalAccuracy

    override val location: Coordinate
        get() = _location

    override val speed: Float
        get() = _speed

    override val altitude: Float
        get() = _altitude

    private val locationManager by lazy { context.getSystemService<LocationManager>() }
    private val cache by lazy { Cache(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val sensorChecker by lazy { SensorChecker(context) }
    private val locationListener = SimpleLocationListener { updateLastLocation(it, true) }

    private var _altitude = 0f
    private var _accuracy: Accuracy = Accuracy.Unknown
    private var _horizontalAccuracy: Float? = null
    private var _verticalAccuracy: Float? = null
    private var _satellites: Int = 0
    private var _speed: Float = 0f
    private var _location = Coordinate.zero

    private var lastLocation: Location? = null

    private var fixStart: Long = 0L
    private val maxFixTime = 8000L

    init {
        _location = Coordinate(
            cache.getFloat(LAST_LATITUDE)?.toDouble() ?: 0.0,
            cache.getFloat(LAST_LONGITUDE)?.toDouble() ?: 0.0
        )
        _altitude = cache.getFloat(LAST_ALTITUDE) ?: 0f
        _speed = cache.getFloat(LAST_SPEED) ?: 0f
    }

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!sensorChecker.hasGPS()) {
            return
        }

        fixStart = System.currentTimeMillis()

        if (lastLocation == null) {
            updateLastLocation(
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                false
            )
        }

        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            20,
            0f,
            locationListener
        )
    }

    override fun stopImpl() {
        locationManager?.removeUpdates(locationListener)
    }

    private fun updateLastLocation(location: Location?, notify: Boolean = true) {
        if (location == null) {
            return
        }

        val satellites = if (location.extras.containsKey("satellites")) location.extras.getInt("satellites") else 0
        val dt = System.currentTimeMillis() - fixStart

        if (useNewLocation(
                lastLocation,
                location
            ) && location.hasAltitude() && location.altitude != 0.0
        ) {
            // Forces an altitude update irrespective of the satellite count - helps when the GPS is being polled in the background
            _altitude = location.altitude.toFloat()

            cache.putFloat(LAST_ALTITUDE, _altitude)

            if (userPrefs.useAltitudeOffsets) {
                _altitude -= AltitudeCorrection.getOffset(_location, context)
            }
        }

        if (satellites < 4 && dt < maxFixTime) {
            return
        }

        if (!useNewLocation(lastLocation, location)) {
            if (notify) notifyListeners()
            return
        }

        fixStart = System.currentTimeMillis()
        _satellites = satellites
        lastLocation = location

        cache.putLong(LAST_UPDATE, fixStart)

        if (location.hasAccuracy()) {
            this._accuracy = when {
                location.accuracy < 8 -> Accuracy.High
                location.accuracy < 16 -> Accuracy.Medium
                else -> Accuracy.Low
            }
            this._horizontalAccuracy = location.accuracy
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (location.hasVerticalAccuracy()) {
                this._verticalAccuracy = location.verticalAccuracyMeters
            }
        }

        if (location.hasSpeed()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
                _speed = if (location.speed < location.speedAccuracyMetersPerSecond * 0.68) {
                    0f
                } else {
                    location.speed
                }

                cache.putFloat(LAST_SPEED, _speed)
            } else {
                this._speed = location.speed
                cache.putFloat(LAST_SPEED, _speed)
            }


        }

        this._location = Coordinate(
            location.latitude,
            location.longitude
        )

        if (location.hasAltitude() && location.altitude != 0.0) {
            _altitude = location.altitude.toFloat()

            cache.putFloat(LAST_ALTITUDE, _altitude)

            if (userPrefs.useAltitudeOffsets) {
                _altitude -= AltitudeCorrection.getOffset(_location, context)
            }
        }

        cache.putFloat(LAST_LATITUDE, location.latitude.toFloat())
        cache.putFloat(LAST_LONGITUDE, location.longitude.toFloat())

        if (notify) notifyListeners()
    }

    private fun hadRecentValidReading(): Boolean {
        val last = cache.getLong(LAST_UPDATE) ?: 0L
        val now = System.currentTimeMillis()
        val recentThreshold = 1000 * 60 * 2L
        return now - last <= recentThreshold
    }

    private fun useNewLocation(current: Location?, newLocation: Location): Boolean {
        // Modified from https://stackoverflow.com/questions/10588982/retrieving-of-satellites-used-in-gps-fix-from-android
        if (current == null) {
            return true
        }

        val timeDelta = newLocation.time - current.time
        val isSignificantlyNewer: Boolean = timeDelta > 1000 * 60 * 2
        val isSignificantlyOlder: Boolean = timeDelta < -1000 * 60 * 2
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }

        val accuracyDelta = (newLocation.accuracy - current.accuracy).toInt()
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 30

        if (isMoreAccurate) {
            return true
        }

        return isNewer && !isSignificantlyLessAccurate
    }

    companion object {
        const val LAST_LATITUDE = "last_latitude"
        const val LAST_LONGITUDE = "last_longitude"
        const val LAST_ALTITUDE = "last_altitude"
        const val LAST_SPEED = "last_speed"
        const val LAST_UPDATE = "last_update"
    }
}
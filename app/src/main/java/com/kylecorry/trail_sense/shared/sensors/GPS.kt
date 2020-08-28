package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.GpsStatus
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.domain.Coordinate


class GPS(private val context: Context) : AbstractSensor(), IGPS {

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

    private val locationManager = context.getSystemService<LocationManager>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val userPrefs = UserPreferences(context)
    private val sensorChecker = SensorChecker(context)
    private val locationListener = SimpleLocationListener { updateLastLocation(it, true) }

    private var _altitude = prefs.getFloat(LAST_ALTITUDE, 0f)
    private var _accuracy: Accuracy = Accuracy.Unknown
    private var _horizontalAccuracy: Float? = null
    private var _verticalAccuracy: Float? = null
    private var _satellites: Int = 0
    private var _speed: Float = prefs.getFloat(LAST_SPEED, 0f)
    private var _location = Coordinate(
        prefs.getFloat(LAST_LATITUDE, 0f).toDouble(),
        prefs.getFloat(LAST_LONGITUDE, 0f).toDouble()
    )

    private var lastLocation: Location? = null

    private var fixStart: Long = 0L
    private val maxFixTime = 8000L

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

        val satellites = location.extras.getInt("satellites")
        val dt = System.currentTimeMillis() - fixStart

        if (satellites < 4 && dt < maxFixTime){
            return
        }

        if (!useNewLocation(lastLocation, location)) {
            if (notify) notifyListeners()
            return
        }

        fixStart = System.currentTimeMillis()
        _satellites = satellites
        lastLocation = location

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
            this._speed = location.speed
            prefs.edit {
                putFloat(LAST_SPEED, _speed)
            }
        }

        this._location = Coordinate(
            location.latitude,
            location.longitude
        )

        if (location.hasAltitude() && location.altitude != 0.0) {
            _altitude = location.altitude.toFloat()

            prefs.edit {
                putFloat(LAST_ALTITUDE, _altitude)
            }

            if (userPrefs.useAltitudeOffsets && userPrefs.useAutoAltitude){
                val offset = AltitudeCorrection.getOffset(this._location, context)
                _altitude -= offset
            }
        }

        prefs.edit {
            putFloat(LAST_LATITUDE, location.latitude.toFloat())
            putFloat(LAST_LONGITUDE, location.longitude.toFloat())
        }

        if (notify) notifyListeners()
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
        private const val LAST_LATITUDE = "last_latitude"
        private const val LAST_LONGITUDE = "last_longitude"
        private const val LAST_ALTITUDE = "last_altitude"
        private const val LAST_SPEED = "last_speed"
    }
}
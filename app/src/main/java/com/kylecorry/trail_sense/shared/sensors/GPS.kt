package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.domain.Coordinate

class GPS(private val context: Context): AbstractSensor(), IGPS {

    override val accuracy: Accuracy
        get() = _accuracy

    override val location: Coordinate
        get() = _location

    override val altitude: Float
        get() = _altitude

    private val locationManager = context.getSystemService<LocationManager>()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensorChecker = SensorChecker(context)
    private val locationListener =
        SimpleLocationListener(this::updateLastLocation)

    private var _altitude = prefs.getFloat(LAST_ALTITUDE, 0f)
    private var _accuracy: Accuracy = Accuracy.Unknown
    private var _location = Coordinate(
        prefs.getFloat(LAST_LATITUDE, 0f).toDouble(),
        prefs.getFloat(LAST_LONGITUDE, 0f).toDouble()
    )

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        if (!sensorChecker.hasGPS()) {
            return
        }
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, locationListener)
    }

    override fun stopImpl() {
        locationManager?.removeUpdates(locationListener)
    }

    private fun updateLastLocation(location: Location?){
        if (location == null){
            return
        }

        if (location.hasAccuracy()) {
            this._accuracy = when {
                location.accuracy < 8 -> Accuracy.High
                location.accuracy < 16 -> Accuracy.Medium
                else -> Accuracy.Low
            }
        }

        this._location = Coordinate(
            location.latitude,
            location.longitude
        )

        if (location.hasAltitude() && location.altitude != 0.0) {
            _altitude = location.altitude.toFloat() - AltitudeCorrection.getOffset(this._location, context)
            prefs.edit {
                putFloat(LAST_ALTITUDE, _altitude)
            }
        }

        prefs.edit {
            putFloat(LAST_LATITUDE, location.latitude.toFloat())
            putFloat(LAST_LONGITUDE, location.longitude.toFloat())
        }

        notifyListeners()
    }

    companion object {
        private const val LAST_LATITUDE = "last_latitude"
        private const val LAST_LONGITUDE = "last_longitude"
        private const val LAST_ALTITUDE = "last_altitude"
    }

}
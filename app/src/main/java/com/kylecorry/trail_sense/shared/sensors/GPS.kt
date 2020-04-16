package com.kylecorry.trail_sense.shared.sensors2

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.Coordinate

class GPS(context: Context): AbstractSensor(), IGPS {

    override val location: Coordinate
        get() = _location

    override val altitude: Float
        get() = _altitude

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val locationListener =
        SimpleLocationListener(this::updateLastLocation)

    private var _altitude = prefs.getFloat(LAST_ALTITUDE, 0f)

    private var _location = Coordinate(
        prefs.getFloat(LAST_LATITUDE, 0f).toDouble(),
        prefs.getFloat(LAST_LONGITUDE, 0f).toDouble()
    )

    @SuppressLint("MissingPermission")
    override fun startImpl() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
    }

    override fun stopImpl() {
        locationManager.removeUpdates(locationListener)
    }

    private fun updateLastLocation(location: Location?){
        if (location == null){
            return
        }

        this._location = Coordinate(location.latitude, location.longitude)

        if (location.hasAltitude() && location.altitude != 0.0) {
            _altitude = location.altitude.toFloat()
        }

        prefs.edit {
            putFloat(LAST_LATITUDE, location.latitude.toFloat())
            putFloat(LAST_LONGITUDE, location.longitude.toFloat())
            if (location.hasAltitude() && location.altitude != 0.0) {
                putFloat(LAST_ALTITUDE, location.altitude.toFloat())
            }
        }

        notifyListeners()
    }

    companion object {
        private const val LAST_LATITUDE = "last_latitude"
        private const val LAST_LONGITUDE = "last_longitude"
        private const val LAST_ALTITUDE = "last_altitude"
    }

}
package com.kylecorry.trail_sense.shared.sensors.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ISensor
import java.time.Duration
import java.time.Instant
import java.util.*

class GPS(ctx: Context): IGPS, ISensor, Observable() {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    private val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var started = false
    private val locationListener = SimpleLocationListener(this::updateLastLocation)

    /**
     * The last known location received by the GPS
     */
    override val location: Coordinate
        get() = _location

    /**
     * The altitude in meters
     */
    override val altitude: AltitudeReading
        get() = _altitude

    private var _altitude = AltitudeReading(Instant.now(), prefs.getFloat(LAST_ALTITUDE, 0f))

    private var _location = Coordinate(
        prefs.getFloat(LAST_LATITUDE, 0f).toDouble(),
        prefs.getFloat(LAST_LONGITUDE, 0f).toDouble()
    )


    init {
        // Set the current location to the last location seen
        @SuppressLint("MissingPermission")
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        updateLastLocation(lastLocation)
    }

    /**
     * Updates the current location
     */
    @SuppressLint("MissingPermission")
    fun updateLocation(onCompleteFunction: ((location: Coordinate?) -> Unit)? = null){
        val that = this
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, SimpleLocationListener {
            updateLastLocation(it)
            onCompleteFunction?.invoke(that.location)
        }, Looper.getMainLooper())
    }

    /**
     * Updates the last location
     * @param location the new location
     */
    private fun updateLastLocation(location: Location?){
        if (location != null) {

            this._location = Coordinate(
                location.latitude,
                location.longitude
            )

            if (location.hasAltitude() && location.altitude != 0.0) {
                _altitude = AltitudeReading(
                    Instant.now(),
                    location.altitude.toFloat()
                )
            }

            prefs.edit {
                putFloat(LAST_LATITUDE, location.latitude.toFloat())
                putFloat(LAST_LONGITUDE, location.longitude.toFloat())
                if (location.hasAltitude() && location.altitude != 0.0) {
                    putFloat(LAST_ALTITUDE, location.altitude.toFloat())
                }
            }

        }
        setChanged()
        notifyObservers()
    }

    override fun start() {
        start(Duration.ofSeconds(5))
    }

    /**
     * Start receiving location updates
     */
    @SuppressLint("MissingPermission")
    fun start(interval: Duration){
        if (started) return
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval.toMillis(), 0f, locationListener)
        started = true
    }

    /**
     * Stop receiving location updates
     */
    override fun stop(){
        if (!started) return
        locationManager.removeUpdates(locationListener)
        started = false
    }

    companion object {
        private const val LAST_LATITUDE = "last_latitude"
        private const val LAST_LONGITUDE = "last_longitude"
        private const val LAST_ALTITUDE = "last_altitude"
    }

}
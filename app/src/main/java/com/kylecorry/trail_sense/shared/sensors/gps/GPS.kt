package com.kylecorry.trail_sense.shared.sensors.gps

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ISensor
import java.time.Duration
import java.time.Instant
import java.util.*

class GPS(ctx: Context): IGPS, ISensor, Observable() {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            result ?: return
            result.lastLocation ?: return
            updateLastLocation(result.lastLocation)
        }
    }

    private var started = false


    init {
        // Set the current location to the last location seen
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                updateLastLocation(it)
            }
        }
    }

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

    /**
     * Updates the current location
     */
    fun updateLocation(onCompleteFunction: ((location: Coordinate?) -> Unit)? = null){
        val callback = object: LocationCallback(){
            override fun onLocationResult(result: LocationResult?) {

                // Log the location result
                locationCallback.onLocationResult(result)

                // Stop future updates
                fusedLocationClient.removeLocationUpdates(this)

                // Notify the callback
                val loc = location
                if (onCompleteFunction != null) onCompleteFunction(loc)
            }
        }

        // Request a single location update
        val locationRequest = LocationRequest.create()?.apply {
            numUpdates = 1
            interval = Duration.ofSeconds(1).toMillis()
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
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
            _altitude = AltitudeReading(
                Instant.now(),
                location.altitude.toFloat()
            )

            prefs.edit {
                putFloat(LAST_LATITUDE, location.latitude.toFloat())
                putFloat(LAST_LONGITUDE, location.longitude.toFloat())
                putFloat(LAST_ALTITUDE, location.altitude.toFloat())
            }

        }
        setChanged()
        notifyObservers()
    }

    override fun start() {
        start(Duration.ofSeconds(8))
    }

    /**
     * Start receiving location updates
     */
    fun start(interval: Duration){
        if (started) return
        val locationRequest = LocationRequest.create()?.apply {
            this.interval = interval.toMillis()
            fastestInterval = interval.dividedBy(2).toMillis()
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback, Looper.getMainLooper())
        started = true
    }

    /**
     * Stop receiving location updates
     */
    override fun stop(){
        if (!started) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        started = false
    }

    companion object {
        private const val LAST_LATITUDE = "last_latitude";
        private const val LAST_LONGITUDE = "last_longitude";
        private const val LAST_ALTITUDE = "last_altitude";
    }

}
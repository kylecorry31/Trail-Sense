package com.kylecorry.survival_aid.navigator

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.*

class GPS(ctx: Context): Observable() {

    private val SECONDS_TO_MILLIS = 1000L

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
            updateLastLocation(it)
        }
    }

    /**
     * The last known location received by the GPS
     */
    var location: Coordinate? = null
        private set

    /**
     * Updates the current location
     */
    fun updateLocation(){
        val callback = object: LocationCallback(){
            override fun onLocationResult(result: LocationResult?) {

                // Log the location result
                locationCallback.onLocationResult(result)

                // Stop future updates
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        // Request a single location update
        val locationRequest = LocationRequest.create()?.apply {
            numUpdates = 1
            interval = 1 * SECONDS_TO_MILLIS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
    }

    /**
     * Updates the last location
     * @param location the new location
     */
    private fun updateLastLocation(location: Location){
        this.location = Coordinate(location.latitude.toFloat(), location.longitude.toFloat())
        setChanged()
        notifyObservers()
    }

    /**
     * Start receiving location updates
     */
    fun start(){
        if (started) return
        val locationRequest = LocationRequest.create()?.apply {
            interval = 8 * SECONDS_TO_MILLIS
            fastestInterval = 2 * SECONDS_TO_MILLIS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback, Looper.getMainLooper())
        started = true
    }

    /**
     * Stop receiving location updates
     */
    fun stop(){
        if (!started) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        started = false
    }

}
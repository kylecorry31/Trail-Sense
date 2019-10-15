package com.kylecorry.survival_aid.navigation

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.*

class GPS(ctx: Context): Observable() {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            result ?: return
            result.lastLocation ?: return
            val lastLocation = result.lastLocation
            lastCoordinate = Coordinate(lastLocation.latitude.toFloat(), lastLocation.longitude.toFloat())
            setChanged()
            notifyObservers()
        }
    }

    private var started = false


    init {
        updateLastLocation()
    }

    private var lastCoordinate: Coordinate? = null

    val location: Coordinate?
        get(){
            return lastCoordinate
        }


    private fun updateLastLocation(){
        fusedLocationClient.lastLocation.addOnSuccessListener {
            location: Location? -> if (location != null) lastCoordinate = Coordinate(location.latitude.toFloat(), location.longitude.toFloat())
        }
    }

    fun start(){
        if (started) return
        val locationRequest = LocationRequest.create()?.apply {
            interval = 15000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback, Looper.getMainLooper())
        started = true
    }

    fun stop(){
        if (!started) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        started = false
    }

}
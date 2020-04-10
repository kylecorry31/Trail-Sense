package com.kylecorry.trail_sense.shared.sensors.gps

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

class SimpleLocationListener(private val onLocationChangedFn: (location: Location?) -> Unit): LocationListener {
    override fun onLocationChanged(location: Location?) {
        onLocationChangedFn.invoke(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }
}
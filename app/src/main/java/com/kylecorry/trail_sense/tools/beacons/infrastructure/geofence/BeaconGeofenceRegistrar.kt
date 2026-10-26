package com.kylecorry.trail_sense.tools.beacons.infrastructure.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.core.net.toUri
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class BeaconGeofenceRegistrar(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val store = BeaconGeofenceStore(context)

    fun isEnabled(beaconId: Long): Boolean {
        return store.isEnabled(beaconId)
    }

    fun setEnabled(beacon: Beacon, enabled: Boolean) {
        store.setEnabled(beacon.id, enabled)
        sync(beacon)
    }

    suspend fun syncAll() {
        BeaconService(context)
            .getBeacons(null, includeGroups = false, maxDepth = null)
            .filterIsInstance<Beacon>()
            .forEach { sync(it) }
    }

    fun remove(beacon: Beacon) {
        store.setEnabled(beacon.id, false)
        locationManager.removeProximityAlert(pendingIntent(beacon))
    }

    private fun sync(beacon: Beacon) {
        if (store.isEnabled(beacon.id) && !beacon.temporary) {
            register(beacon)
        } else {
            locationManager.removeProximityAlert(pendingIntent(beacon))
        }
    }

    private fun register(beacon: Beacon) {
        if (!Permissions.isBackgroundLocationEnabled(context)) {
            return
        }

        // Stubbed: the radius is the same for every beacon
        locationManager.addProximityAlert(
            beacon.coordinate.latitude,
            beacon.coordinate.longitude,
            GEOFENCE_RADIUS_METERS,
            -1L,
            pendingIntent(beacon)
        )
    }

    // Must be mutable because the system adds the entering/exiting extra before sending it.
    // The data URI makes each beacon's PendingIntent unique.
    private fun pendingIntent(beacon: Beacon): PendingIntent {
        val intent = Intent(context, BeaconGeofenceReceiver::class.java).apply {
            data = "trailsense://beacon-geofence/${beacon.id}".toUri()
            putExtra(BeaconGeofenceReceiver.EXTRA_BEACON_NAME, beacon.name)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    companion object {
        const val GEOFENCE_RADIUS_METERS = 100f
    }
}

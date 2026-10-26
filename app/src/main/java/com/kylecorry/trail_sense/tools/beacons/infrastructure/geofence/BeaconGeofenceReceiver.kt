package com.kylecorry.trail_sense.tools.beacons.infrastructure.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.tools.beacons.BeaconsToolRegistration

class BeaconGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
            return
        }

        val name = intent.getStringExtra(EXTRA_BEACON_NAME) ?: return
        val notification = Notify.alert(
            context,
            BeaconsToolRegistration.NOTIFICATION_CHANNEL_GEOFENCE,
            "Near $name",
            "You are within ${BeaconGeofenceRegistrar.GEOFENCE_RADIUS_METERS.toInt()} m of this beacon",
            R.drawable.ic_location,
            autoCancel = true
        )
        DependencyRegistry.get<NotificationSubsystem>()
            .send(intent.data.hashCode(), notification)
    }

    companion object {
        const val EXTRA_BEACON_NAME = "beacon_name"
    }
}

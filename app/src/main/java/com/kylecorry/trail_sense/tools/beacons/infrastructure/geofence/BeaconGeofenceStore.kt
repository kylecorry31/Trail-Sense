package com.kylecorry.trail_sense.tools.beacons.infrastructure.geofence

import android.content.Context
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem

// Stubbed: stored in session state preferences instead of the beacon database
class BeaconGeofenceStore(context: Context) {

    private val prefs = SessionStateSubsystem.getInstance(context).preferences

    fun isEnabled(beaconId: Long): Boolean {
        return beaconId in getEnabledIds()
    }

    fun setEnabled(beaconId: Long, enabled: Boolean) {
        val ids = getEnabledIds()
        val updated = if (enabled) ids + beaconId else ids - beaconId
        prefs.putString(KEY_ENABLED_IDS, updated.joinToString(","))
    }

    private fun getEnabledIds(): Set<Long> {
        return prefs.getString(KEY_ENABLED_IDS)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    companion object {
        private const val KEY_ENABLED_IDS = "beacon_geofence_enabled_ids"
    }
}

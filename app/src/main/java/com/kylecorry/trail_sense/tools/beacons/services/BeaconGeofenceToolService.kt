package com.kylecorry.trail_sense.tools.beacons.services

import android.content.Context
import com.kylecorry.trail_sense.tools.beacons.BeaconsToolRegistration
import com.kylecorry.trail_sense.tools.beacons.infrastructure.geofence.BeaconGeofenceRegistrar
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import java.time.Duration

// Stubbed: always enabled, there is no global on/off setting
class BeaconGeofenceToolService(private val context: Context) : ToolService {

    override val id: String = BeaconsToolRegistration.SERVICE_GEOFENCE

    override val name: String = "Beacon geofences"

    override fun getFrequency(): Duration = Duration.ofDays(1)

    override fun isRunning(): Boolean = true

    override fun isEnabled(): Boolean = true

    override fun isBlocked(): Boolean = false

    override suspend fun enable() = restart()

    override suspend fun disable() {}

    // Proximity alerts do not survive a reboot, so they are registered again on every restart
    override suspend fun restart() {
        BeaconGeofenceRegistrar(context).syncAll()
    }

    override suspend fun stop() {}
}

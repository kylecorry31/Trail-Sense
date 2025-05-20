package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Navigator private constructor(context: Context) {

    private val prefs = PreferencesSubsystem.getInstance(context).preferences
    private val service = BeaconService(context)

    // Flows
    private val _destinationId = MutableStateFlow(getDestinationId())
    private val destinationId: Flow<Long?> = _destinationId
        .distinctUntilChanged { old, new -> old == new }

    val destination = destinationId.map { it?.let { service.getBeacon(it) } }

    fun navigateTo(location: Coordinate, name: String = "", owner: BeaconOwner = BeaconOwner.User) {
        val beacon = Beacon.temporary(location, name = name, visible = false, owner = owner)
        CoroutineScope(Dispatchers.IO).launch {
            val id = service.add(beacon)
            navigateTo(id)
        }
    }

    fun navigateTo(beacon: Beacon) {
        navigateTo(beacon.id)
    }

    fun navigateTo(beaconId: Long) {
        prefs.putLong(DESTINATION_ID_KEY, beaconId)
        _destinationId.update { beaconId }
    }

    fun cancelNavigation() {
        prefs.remove(DESTINATION_ID_KEY)
        _destinationId.update { null }
    }

    fun getDestinationId(): Long? {
        return prefs.getLong(DESTINATION_ID_KEY)
    }

    suspend fun getDestination(): Beacon? = onIO {
        val id = getDestinationId() ?: return@onIO null
        service.getBeacon(id)
    }

    fun isNavigating(): Boolean {
        return getDestinationId() != null
    }

    companion object {
        const val DESTINATION_ID_KEY = "last_beacon_id_long"

        private var instance: Navigator? = null

        @Synchronized
        fun getInstance(context: Context): Navigator {
            if (instance == null) {
                instance = Navigator(context.applicationContext)
            }
            return instance!!
        }
    }

}
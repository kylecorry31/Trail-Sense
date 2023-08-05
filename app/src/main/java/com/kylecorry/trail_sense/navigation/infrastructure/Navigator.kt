package com.kylecorry.trail_sense.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class Navigator private constructor(context: Context) {

    private val prefs = PreferencesSubsystem.getInstance(context).preferences
    private val repo = BeaconRepo.getInstance(context)

    // Flows
    private val _destinationId = MutableStateFlow(getDestinationId())
    val destinationId: Flow<Long?> = _destinationId
        .distinctUntilChanged { old, new -> old == new }

    val destination = destinationId.map { it?.let { repo.getBeacon(it)?.toBeacon() } }

    fun navigateTo(beacon: Beacon) {
        navigateTo(beacon.id)
    }

    fun navigateTo(beaconId: Long) {
        prefs.putLong(DESTINATION_ID_KEY, beaconId)
        _destinationId.tryEmit(beaconId)
    }

    fun cancelNavigation() {
        prefs.remove(DESTINATION_ID_KEY)
        _destinationId.tryEmit(null)
    }

    fun getDestinationId(): Long? {
        return prefs.getLong(DESTINATION_ID_KEY)
    }

    suspend fun getDestination(): Beacon? = onIO {
        val id = getDestinationId() ?: return@onIO null
        repo.getBeacon(id)?.toBeacon()
    }

    companion object {
        val DESTINATION_ID_KEY = "last_beacon_id_long"

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
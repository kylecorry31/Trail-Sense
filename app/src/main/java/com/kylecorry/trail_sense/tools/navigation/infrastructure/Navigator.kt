package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class Navigator private constructor(context: Context) {

    private val prefs = PreferencesSubsystem.getInstance(context).preferences
    private val beacons = BeaconService(context)
    private val bearings = NavigationBearingService.getInstance(context)

    // Flows
    private val _destinationId = MutableStateFlow(getDestinationId())
    private val _forceUpdate = MutableStateFlow(0)
    private val destinationId: Flow<Long?> = _destinationId

    val destination = destinationId
        .combine(_forceUpdate) { id, _ -> id }
        .map { it?.let { beacons.getBeacon(it) } }

    fun navigateTo(
        location: Coordinate,
        name: String = "",
        owner: BeaconOwner = BeaconOwner.User,
        elevation: Float? = null,
        useDemElevation: Boolean = elevation == null,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val beacon = Beacon.temporary(
                location,
                elevation = if (useDemElevation) DEM.getElevation(location) else elevation,
                name = name,
                visible = false,
                owner = owner
            )
            val id = beacons.add(beacon)
            navigateTo(id)
        }
    }

    fun navigateTo(beacon: Beacon) {
        navigateTo(beacon.id)
    }

    fun navigateTo(beaconId: Long) {
        prefs.putLong(DESTINATION_ID_KEY, beaconId)
        _destinationId.update { beaconId }
        _forceUpdate.update { it -> it + 1 }
    }

    fun cancelNavigation() {
        prefs.remove(DESTINATION_ID_KEY)
        _destinationId.update { null }
        _forceUpdate.update { it -> it + 1 }
    }

    fun getDestinationId(): Long? {
        return prefs.getLong(DESTINATION_ID_KEY)
    }

    suspend fun getDestination(): Beacon? = onIO {
        val id = getDestinationId() ?: return@onIO null
        beacons.getBeacon(id)
    }

    fun isNavigating(): Boolean {
        return getDestinationId() != null
    }

    // TODO: Replace isNavigating with this
    suspend fun isNavigating2(): Boolean {
        return getDestination() != null && bearings.isNavigating()
    }

    // TODO: Merge this with beacon navigation
    // Bearings
    val navigationBearing= bearings.getBearing()

    suspend fun navigateToBearing(bearing: Float, startingLocation: Coordinate? = null) {
        val navigationBearing = NavigationBearing(
            0,
            bearing,
            startingLocation,
            isActive = true,
            startTime = Instant.now()
        )
        bearings.setBearing(navigationBearing)
    }

    suspend fun clearBearing() {
        bearings.setBearing(null)
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
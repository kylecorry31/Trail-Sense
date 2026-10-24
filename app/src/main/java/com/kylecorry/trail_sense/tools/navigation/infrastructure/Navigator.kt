package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingService
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class Navigator private constructor(context: Context) {

    private val prefs = SessionStateSubsystem.getInstance(context).preferences
    private val userPrefs = DependencyRegistry.get<UserPreferences>()
    private val beacons = BeaconService(context)
    private val bearings = NavigationBearingService.getInstance(context)
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()

    // Flows
    private val beaconDestinationId = MutableStateFlow(prefs.getLong(BEACON_DESTINATION_ID_KEY))
    private val beaconDestinationInvalidationKey = MutableStateFlow(0)

    // Bearings
    private val navigationBearing = bearings.getBearing()

    private val bearingDestination = navigationBearing.map {
        it?.let {
            Destination.Bearing(
                Bearing.from(it.bearing),
                userPrefs.compass.useTrueNorth,
                getDeclination(it.startLocation ?: locationSubsystem.location),
                it.startLocation,
                userPrefs.navigation.bearingDistance
            )
        }
    }

    // TODO: Standardized navigation instructions
    // NavigationInstructions
    // Bearing
    // Distance?
    // ETA?
    // Elevation change?
    // Beacons = always to the beacon, bearings = if off track, then back to the bearing reading otherwise bearing end point (or just the bearing if no location info)

    private val beaconDestination = combine(beaconDestinationId, beaconDestinationInvalidationKey) { id, _ ->
        id?.let { beacons.getBeacon(it) }?.let { Destination.Beacon(it) }
    }

    private val pathNavigator = PathNavigator(context)
    val isRestoringNavigation = pathNavigator.isRestoring

    val destination =
        combine(bearingDestination, beaconDestination, pathNavigator.destination) { bearing, beacon, path ->
            path ?: beacon ?: bearing
        }

    suspend fun navigateAlongPath(path: Path, follow: PathFollow) {
        pathNavigator.navigate(path, follow)
        clearBeaconDestination()
        clearBearing()
        Tools.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    private val listenTask = BackgroundTask {
        destination.collect {
            Tools.broadcast(NavigationToolRegistration.BROADCAST_DESTINATION_CHANGED)
        }
    }

    init {
        listenTask.start()
        // Keeps the beacon destination in sync with external changes to the preferences
        prefs.onChange.subscribe { key ->
            if (key == BEACON_DESTINATION_ID_KEY) {
                val id = prefs.getLong(BEACON_DESTINATION_ID_KEY)
                if (beaconDestinationId.value != id) {
                    updateBeaconDestination(id)
                }
            }
            true
        }
    }

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
                elevation = if (useDemElevation) DEM.getElevation(location).elevation else elevation,
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
        pathNavigator.cancel()
        CoroutineScope(Dispatchers.IO).launch { clearBearing() }
        setBeaconDestination(beaconId)
        Tools.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    private fun setBeaconDestination(beaconId: Long) {
        updateBeaconDestination(beaconId)
        prefs.putLong(BEACON_DESTINATION_ID_KEY, beaconId)
    }

    fun cancelBeaconNavigation() {
        val wasNavigating = getBeaconDestinationId() != null
        clearBeaconDestination()
        if (wasNavigating) {
            Tools.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }
    }

    private fun clearBeaconDestination() {
        updateBeaconDestination(null)
        prefs.remove(BEACON_DESTINATION_ID_KEY)
    }

    private fun updateBeaconDestination(beaconId: Long?) {
        beaconDestinationId.value = beaconId
        beaconDestinationInvalidationKey.update { it + 1 }
    }

    fun cancelPathNavigation(pathId: Long) {
        if (pathNavigator.cancel(pathId)) {
            Tools.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }
    }

    /**
     * Cancels all navigation.
     * @return a function which restores the navigation that was cancelled, unless the user has
     * started navigating somewhere else since
     */
    suspend fun cancelAllNavigation(): suspend () -> Unit {
        val beaconId = getBeaconDestinationId()
        val bearing = navigationBearing.firstOrNull()
        val pathSession = pathNavigator.currentSession()

        pathNavigator.cancel()
        clearBeaconDestination()
        clearBearing()

        if (beaconId != null || bearing != null || pathSession != null) {
            Tools.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }

        return restore@{
            if (isNavigating()) {
                return@restore
            }
            beaconId?.let { setBeaconDestination(it) }
            bearing?.let { bearings.setBearing(it) }
            pathSession?.let { pathNavigator.restore(it) }
        }
    }

    private suspend fun isNavigating(): Boolean {
        return getBeaconDestinationId() != null ||
                navigationBearing.firstOrNull() != null ||
                pathNavigator.currentSession() != null
    }

    fun getBeaconDestinationId(): Long? {
        return beaconDestinationId.value
    }

    suspend fun getDestination(): Destination? {
        pathNavigator.awaitRestore()
        return destination.firstOrNull()
    }

    suspend fun navigateToBearing(bearing: Float, startingLocation: Coordinate? = null) {
        pathNavigator.cancel()
        clearBeaconDestination()
        val navigationBearing = NavigationBearing(
            0,
            bearing,
            startingLocation,
            isActive = true,
            startTime = Instant.now()
        )
        bearings.setBearing(navigationBearing)
        Tools.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    suspend fun clearBearing() {
        bearings.setBearing(null)
    }

    fun getBearing(
        myLocation: Coordinate,
        destination: Destination,
        declination: Float
    ): Bearing {
        val target = when (destination) {
            is Destination.Path -> destination.route.guidance.target
            is Destination.Beacon -> destination.beacon.coordinate
            is Destination.Bearing -> {
                if (destination.startingLocation == null || !userPrefs.navigation.lockBearingToLocation) {
                    return destination.bearing
                }
                destination.targetLocation!!
            }
        }

        return fromTrueNorth(
            myLocation.bearingTo(target),
            userPrefs.compass.useTrueNorth,
            declination
        )
    }

    private fun getDeclination(location: Coordinate): Float {
        return if (userPrefs.useAutoDeclination) {
            Geophysics.getGeomagneticDeclination(
                location,
                locationSubsystem.elevation.meters().value
            )
        } else {
            userPrefs.declinationOverride
        }
    }

    private fun fromTrueNorth(
        bearing: Bearing,
        useTrueNorth: Boolean,
        declination: Float
    ): Bearing {
        if (useTrueNorth) {
            return bearing
        }
        return DeclinationUtils.fromTrueNorthBearing(bearing, declination)
    }

    companion object {
        internal const val BEACON_DESTINATION_ID_KEY = "last_beacon_id_long"

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

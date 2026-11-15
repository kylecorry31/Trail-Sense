package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingCalculator
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.GuidanceInput
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolEventEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

internal interface NavigatorSettings {
    val useTrueNorth: Boolean
    val lockBearingToLocation: Boolean
    val bearingDistance: Distance
    val useAutoDeclination: Boolean
    val declinationOverride: Float
}

class Navigator internal constructor(
    private val prefs: IPreferences,
    private val settings: NavigatorSettings,
    private val beacons: IBeaconService,
    private val bearings: NavigationBearingService,
    private val pathNavigator: PathNavigator,
    private val currentLocation: () -> Coordinate,
    private val currentElevation: () -> Distance,
    private val eventBus: IEventEmitter,
    subscribeToLockBearingChanges: (onChanged: () -> Unit) -> Unit
) {

    private constructor(context: Context) : this(
        SessionStateSubsystem.getInstance(context).preferences,
        UserPreferencesNavigatorSettings(DependencyRegistry.get<UserPreferences>()),
        BeaconService(context),
        NavigationBearingService.getInstance(context),
        PathNavigator(context),
        DependencyRegistry.get<LocationSubsystem>().let { subsystem -> { subsystem.location } },
        DependencyRegistry.get<LocationSubsystem>().let { subsystem -> { subsystem.elevation } },
        ToolEventEmitter,
        { onChanged ->
            val lockBearingKey = context.getString(R.string.pref_lock_bearing_to_location)
            PreferencesSubsystem.getInstance(context).preferences.onChange.subscribe { key ->
                if (key == lockBearingKey) {
                    onChanged()
                }
                true
            }
        }
    )

    // Flows
    private val beaconDestinationId = MutableStateFlow(prefs.getLong(BEACON_DESTINATION_ID_KEY))
    private val beaconDestinationInvalidationKey = MutableStateFlow(0)

    // Bearings
    private val navigationBearing = bearings.getBearing()

    private val bearingInvalidationKey = MutableStateFlow(0)

    private val bearingDestination = combine(navigationBearing, bearingInvalidationKey) { bearing, _ ->
        bearing?.let {
            // The start location is hidden when the bearing is not locked to it, so it is not seen by consumers
            val startLocation = it.startLocation.takeIf { settings.lockBearingToLocation }
            Destination.Bearing(
                Bearing.from(it.bearing),
                settings.useTrueNorth,
                getDeclination(startLocation ?: currentLocation()),
                startLocation,
                settings.bearingDistance
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

    val isRestoringNavigation = pathNavigator.isRestoring

    val destination =
        combine(bearingDestination, beaconDestination, pathNavigator.destination) { bearing, beacon, path ->
            path ?: beacon ?: bearing
        }

    suspend fun navigateAlongPath(path: Path, follow: PathFollow) {
        pathNavigator.navigate(path, follow)
        clearBeaconDestination()
        clearBearing()
        eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    private val listenTask = BackgroundTask {
        destination.collect {
            eventBus.broadcast(NavigationToolRegistration.BROADCAST_DESTINATION_CHANGED)
        }
    }

    init {
        listenTask.start()
        subscribeToLockBearingChanges {
            bearingInvalidationKey.update { it + 1 }
        }
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
                elevation = if (useDemElevation) {
                    DEM.getElevation(location).takeIf { it.hasData }?.elevation
                } else {
                    elevation
                },
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
        eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    private fun setBeaconDestination(beaconId: Long) {
        updateBeaconDestination(beaconId)
        prefs.putLong(BEACON_DESTINATION_ID_KEY, beaconId)
    }

    fun cancelBeaconNavigation() {
        val wasNavigating = getBeaconDestinationId() != null
        clearBeaconDestination()
        if (wasNavigating) {
            eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
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
            eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
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
            eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }

        return restore@{
            if (isNavigating()) {
                return@restore
            }
            beaconId?.let { setBeaconDestination(it) }
            bearing?.let { bearings.setBearing(it) }
            pathSession?.let { pathNavigator.restore(it) }
            if (beaconId != null || bearing != null || pathSession != null) {
                eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
            }
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
        eventBus.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    suspend fun clearBearing() {
        bearings.setBearing(null)
    }

    fun getBearing(
        myLocation: Coordinate,
        destination: Destination,
        declination: Float
    ): Bearing {
        return getGuidance(
            destination,
            myLocation,
            currentElevation(),
            declination
        ).targetBearing
    }

    fun getGuidance(
        destination: Destination,
        location: Coordinate,
        elevation: Distance,
        declination: Float,
        useTrueNorthOverride: Boolean? = null
    ): Guidance {
        val useTrueNorth = destination.usesTrueNorth(
            settings.useTrueNorth,
            useTrueNorthOverride
        )
        return destination.getGuidance(
            GuidanceInput(location, elevation, declination, useTrueNorth)
        )
    }

    private fun getDeclination(location: Coordinate): Float {
        return NavigationBearingCalculator.getDeclination(
            location,
            currentElevation().meters().value,
            System.currentTimeMillis(),
            settings.useAutoDeclination,
            settings.declinationOverride
        )
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

private class UserPreferencesNavigatorSettings(private val userPrefs: UserPreferences) : NavigatorSettings {
    override val useTrueNorth get() = userPrefs.compass.useTrueNorth
    override val lockBearingToLocation get() = userPrefs.navigation.lockBearingToLocation
    override val bearingDistance get() = userPrefs.navigation.bearingDistance
    override val useAutoDeclination get() = userPrefs.useAutoDeclination
    override val declinationOverride get() = userPrefs.declinationOverride
}

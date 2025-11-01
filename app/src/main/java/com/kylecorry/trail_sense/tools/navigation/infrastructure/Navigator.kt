package com.kylecorry.trail_sense.tools.navigation.infrastructure

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant

class Navigator private constructor(context: Context) {

    private val prefs = PreferencesSubsystem.getInstance(context).preferences
    private val userPrefs = AppServiceRegistry.get<UserPreferences>()
    private val beacons = BeaconService(context)
    private val bearings = NavigationBearingService.getInstance(context)
    private val locationSubsystem = AppServiceRegistry.get<LocationSubsystem>()

    private val declinationCache = GeospatialCache<Float>(Distance.kilometers(10f), 10)

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

    // Bearings
    val navigationBearing = bearings.getBearing()

    val bearingDestination = navigationBearing.map {
        it?.let {
            val declination = if (userPrefs.useAutoDeclination) {
                Geology.getGeomagneticDeclination(it.startLocation ?: locationSubsystem.location)
            } else {
                userPrefs.declinationOverride
            }
            Destination.Bearing(
                Bearing.from(it.bearing),
                userPrefs.compass.useTrueNorth,
                declination,
                it.startLocation
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

    val beaconDestination = destination.map {
        it?.let {
            Destination.Beacon(it)
        }
    }

    val destination2 = bearingDestination.combine(beaconDestination) { bearing, beacon ->
        beacon ?: bearing
    }

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

    fun getBearing(
        myLocation: Coordinate,
        destination: Destination
    ): Bearing {
        val useTrueNorth = userPrefs.compass.useTrueNorth

        return when (destination) {
            is Destination.Beacon -> {
                fromTrueNorth(
                    myLocation.bearingTo(destination.beacon.coordinate),
                    useTrueNorth,
                    getDeclination(myLocation)
                )
            }

            is Destination.Bearing -> {
                if (destination.startingLocation != null && userPrefs.navigation.useLocationWithBearing) {
                    fromTrueNorth(
                        myLocation.bearingTo(destination.targetLocation!!),
                        useTrueNorth,
                        getDeclination(myLocation)
                    )
                } else {
                    destination.bearing
                }
            }

        }
    }

    private fun getDeclination(location: Coordinate? = null, elevation: Float? = null): Float {
        // TODO: Cache declination
        return if (userPrefs.useAutoDeclination) {
            val actualLocation = location ?: locationSubsystem.location
            runBlocking {
                declinationCache.getOrPut(actualLocation) {
                    Geology.getGeomagneticDeclination(
                        actualLocation,
                        elevation ?: locationSubsystem.elevation.meters().value
                    )
                }
            }
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
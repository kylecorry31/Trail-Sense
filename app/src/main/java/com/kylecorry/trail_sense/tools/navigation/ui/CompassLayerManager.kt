package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.domain.MappableBearing
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.compass.layers.BeaconCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.compass.layers.MarkerCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.compass.layers.NavigationCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.compass.layers.data.UpdateAstronomyLayerCommand
import java.time.Duration

class CompassLayerManager(
    private val gps: IGPS,
    declinationProvider: () -> Float
) {
    // TODO: These should be looked up internally
    var destination: Destination? = null
    var nearbyBeacons: List<Beacon> = emptyList()

    private val beaconCompassLayer = BeaconCompassLayer()
    private val astronomyCompassLayer = MarkerCompassLayer()
    private val navigationCompassLayer = NavigationCompassLayer()
    private val prefs = getAppService<UserPreferences>()
    private val navigator = getAppService<Navigator>()
    private val triggers = HookTriggers()

    private val updateAstronomyLayerCommand = UpdateAstronomyLayerCommand(
        astronomyCompassLayer,
        prefs,
        gps,
        declinationProvider
    )

    private val timer = CoroutineTimer {
        updateCompassLayers()
    }


    fun resume(
        linearCompass: ICompassView,
        radarCompass: ICompassView
    ) {
        linearCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                beaconCompassLayer,
                navigationCompassLayer
            )
        )
        radarCompass.setCompassLayers(
            listOfNotNull(
                astronomyCompassLayer,
                if (prefs.navigation.showNearbyBeaconsOnlyOnLinearCompass) null else beaconCompassLayer,
                navigationCompassLayer
            )
        )
        // TODO: Listen for location updates
        // TODO: This shouldn't be needed - layers can be updated when the data changes
        timer.interval(100)
    }

    fun pause() {
        timer.stop()
    }

    private fun getDestinationBearing(): Float? {
        return destination?.let { navigator.getBearing(gps.location, it).value }
    }

    private suspend fun updateCompassLayers() {
        val destBearing = getDestinationBearing()
        val destinationBeacon = (destination as? Destination.Beacon)?.beacon
        val destColor = destinationBeacon?.color ?: AppColor.Blue.color

        val direction = destBearing?.let {
            MappableBearing(it, destColor)
        }

        // Update beacon layers
        beaconCompassLayer.setBeacons(nearbyBeacons)
        beaconCompassLayer.highlight(destinationBeacon)

        // Destination
        if (destinationBeacon != null) {
            navigationCompassLayer.setDestination(destinationBeacon)
        } else if (direction != null) {
            navigationCompassLayer.setDestination(direction)
        } else {
            navigationCompassLayer.setDestination(null as MappableBearing?)
        }

        // Astronomy layer
        val distanceTriggered = triggers.distance(
            "astronomy",
            gps.location,
            ASTRONOMY_UPDATE_DISTANCE,
            highAccuracy = false
        )
        val timeTriggered = triggers.frequency("astronomy", ASTRONOMY_UPDATE_FREQUENCY)
        if (distanceTriggered || timeTriggered) {
            updateAstronomyData()
        }
    }

    private suspend fun updateAstronomyData() {
        if (gps.location == Coordinate.zero) {
            return
        }

        updateAstronomyLayerCommand.execute()
    }

    companion object {
        private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f).meters()
        private val ASTRONOMY_UPDATE_FREQUENCY = Duration.ofMinutes(1)
    }

}

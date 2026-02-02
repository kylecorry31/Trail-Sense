package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.luna.coroutines.BackgroundTask
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class NavigationLayer :
    GeoJsonLayer<NavigationGeoJsonSource>(NavigationGeoJsonSource(), layerId = LAYER_ID) {

    private val navigator = AppServiceRegistry.get<Navigator>()
    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val locationSubsystem = AppServiceRegistry.get<LocationSubsystem>()
    private val task = BackgroundTask {
        navigator.destination2.collect {
            setDestination(it)
        }
    }

    private val onLocationChanged = { _: Bundle ->
        setMyLocation(locationSubsystem.location)
        true
    }

    override fun start() {
        useLocationWithBearing = prefs.navigation.lockBearingToLocation
        setMyLocation(locationSubsystem.location)
        Tools.subscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED, onLocationChanged)
        task.start()
    }

    override fun stop() {
        Tools.unsubscribe(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED, onLocationChanged)
        task.stop()
    }

    var useLocationWithBearing: Boolean
        get() = source.useLocationWithBearing
        set(value) {
            source.useLocationWithBearing = value
            invalidate()
            notifyListeners()
        }

    fun setMyLocation(location: Coordinate?) {
        source.myLocation = location
        invalidate()
        notifyListeners()
    }

    fun setDestination(destination: Destination?) {
        source.destination = destination
        invalidate()
        notifyListeners()
    }

    companion object {
        const val LAYER_ID = "navigation"
    }
}
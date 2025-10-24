package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationStrategy
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NavigationLayerManager(context: Context, private val layer: NavigationLayer) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val navigator = AppServiceRegistry.get<Navigator>()
    private val prefs = AppServiceRegistry.get<UserPreferences>()

    override fun start() {
        scope.launch {
            // Load destination
            navigator.destination.collect {
                if (it != null) {
                    layer.setNavigation(NavigationStrategy.Beacon(it))
                } else if (!navigator.isNavigating2()) {
                    layer.setNavigation(null)
                }
            }
        }

        scope.launch {
            navigator.navigationBearing.collect {
                if (it != null) {
                    layer.setNavigation(
                        NavigationStrategy.Bearing(
                            Bearing.from(it.bearing),
                            prefs.compass.useTrueNorth, // TODO: This should be saved
                            0f, // TODO: This should be saved
                            it.startLocation
                        )
                    )
                } else if (!navigator.isNavigating2()) {
                    layer.setNavigation(null)
                }
            }
        }
    }

    override fun stop() {
        scope.cancel()
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setMyLocation(location)
    }
}
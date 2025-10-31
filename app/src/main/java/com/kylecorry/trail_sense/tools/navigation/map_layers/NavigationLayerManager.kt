package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager
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
        layer.useLocationWithBearing = prefs.navigation.useLocationWithBearing
        scope.launch {
            navigator.destination2.collect {
                layer.setDestination(it)
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
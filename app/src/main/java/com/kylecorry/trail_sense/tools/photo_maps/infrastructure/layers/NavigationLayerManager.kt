package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import android.content.Context
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NavigationLayerManager(context: Context, private val layer: NavigationLayer) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val navigator = Navigator.getInstance(context)

    override fun start() {
        scope.launch {
            // Load destination
            navigator.destination.collect {
                if (it != null) {
                    val colorWithAlpha = it.color.withAlpha(127)
                    layer.setColor(colorWithAlpha)
                    layer.setEnd(it.coordinate)
                } else {
                    layer.setEnd(null)
                }
            }
        }
    }

    override fun stop() {
        scope.cancel()
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setStart(location)
    }
}
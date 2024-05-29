package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.layers.COGLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class COGLayerManager(private val layer: COGLayer, @ColorInt private val color: Int) :
    BaseLayerManager() {

    override fun start() {
        layer.setColor(color)
    }

    override fun stop() {
    }

    override fun onSpeedChanged(speed: Speed) {
        super.onSpeedChanged(speed)
        layer.setSpeed(speed)
    }

    override fun onCOGChanged(cog: Bearing?) {
        super.onCOGChanged(cog)
        layer.setCOG(cog)
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location)
    }
}
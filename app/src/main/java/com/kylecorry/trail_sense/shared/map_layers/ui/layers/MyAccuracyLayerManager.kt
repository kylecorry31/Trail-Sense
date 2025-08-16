package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate

class MyAccuracyLayerManager(
    private val layer: MyAccuracyLayer,
    @ColorInt private val color: Int
) :
    BaseLayerManager() {
    override fun start() {
        layer.setColors(color, Color.TRANSPARENT)
    }

    override fun stop() {
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location, accuracy)
    }
}
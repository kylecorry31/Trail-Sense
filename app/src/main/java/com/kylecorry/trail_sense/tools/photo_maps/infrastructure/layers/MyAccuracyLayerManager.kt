package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyAccuracyLayer

class MyAccuracyLayerManager(
    private val layer: MyAccuracyLayer,
    @ColorInt private val color: Int,
    private val opacity: Int = 50
) :
    BaseLayerManager() {
    override fun start() {
        layer.setColors(color, Color.TRANSPARENT, opacity)
    }

    override fun stop() {
    }

    override fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        super.onLocationChanged(location, accuracy)
        layer.setLocation(location, accuracy)
    }
}
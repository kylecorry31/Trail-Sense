package com.kylecorry.trail_sense.navigation.ui.layers

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

class BeaconLayer(@ColorInt private val backgroundColor: Int) : ILayer {

    private val _beacons = mutableListOf<Beacon>()

    fun setBeacons(beacons: List<Beacon>) {
        _beacons.clear()
        _beacons.addAll(beacons)
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, mapper: ICoordinateToPixelStrategy, scale: Float) {
        _beacons.forEach { beacon ->
            val pixel = mapper.getPixels(beacon.coordinate)
            drawer.noTint()
            drawer.stroke(backgroundColor)
            drawer.strokeWeight(drawer.dp(0.5f) * scale)
            drawer.fill(beacon.color)
//            if (highlight) {
//                opacity(255)
//            } else {
//                opacity(127)
//            }
            drawer.circle(pixel.x, pixel.y, drawer.dp(10f) * scale)
        }
    }

    override fun invalidate() {
        // Do nothing
    }
}
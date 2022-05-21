package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

class BeaconLayer : ILayer {

    private val _beacons = mutableListOf<Beacon>()

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    fun setBeacons(beacons: List<Beacon>) {
        _beacons.clear()
        _beacons.addAll(beacons)
        invalidate()
    }

    fun setOutlineColor(@ColorInt color: Int) {
        backgroundColor = color
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
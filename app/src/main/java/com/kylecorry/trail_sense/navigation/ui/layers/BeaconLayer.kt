package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

class BeaconLayer : ILayer {

    private val _beacons = mutableListOf<Beacon>()
    private var _highlighted: Beacon? = null

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    fun setBeacons(beacons: List<Beacon>) {
        _beacons.clear()
        _beacons.addAll(beacons)
        invalidate()
    }

    fun setOutlineColor(@ColorInt color: Int) {
        backgroundColor = color
        invalidate()
    }

    fun highlight(beacon: Beacon?){
        _highlighted = beacon
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        val scale = 1f // TODO: Determine this based on map.scale
        _beacons.forEach { beacon ->
            val pixel = map.toPixel(beacon.coordinate)
            drawer.noTint()
            drawer.stroke(backgroundColor)
            drawer.strokeWeight(drawer.dp(0.5f) * scale)
            drawer.fill(beacon.color)
            if (_highlighted == null || _highlighted?.id == beacon.id) {
                drawer.opacity(255)
            } else {
                drawer.opacity(127)
            }
            drawer.circle(pixel.x, pixel.y, drawer.dp(10f) * scale)
        }
    }

    override fun invalidate() {
        // Do nothing
    }
}
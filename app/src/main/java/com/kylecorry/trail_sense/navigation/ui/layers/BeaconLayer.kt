package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.navigation.ui.markers.CircleMapMarker

class BeaconLayer(private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }) :
    BaseLayer() {

    private val _beacons = mutableListOf<Beacon>()
    private var _highlighted: Beacon? = null

    private var _beaconUpToDate = false
    private var _drawer: ICanvasDrawer? = null

    // TODO: Cache bitmaps

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    fun setBeacons(beacons: List<Beacon>) {
        _beacons.clear()
        _beacons.addAll(beacons)
        _beaconUpToDate = false
        updateMarkers()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        _drawer = drawer
        if (!_beaconUpToDate) {
            updateMarkers()
        }
        super.draw(drawer, map)
    }

    fun setOutlineColor(@ColorInt color: Int) {
        backgroundColor = color
        updateMarkers()
    }

    fun highlight(beacon: Beacon?) {
        _highlighted = beacon
        updateMarkers()
    }

    private fun updateMarkers() {
        val drawer = _drawer ?: return
        val size = drawer.dp(8f).toInt()
        _beaconUpToDate = true
        clearMarkers()
        _beacons.forEach {
            val opacity = if (_highlighted == null || _highlighted?.id == it.id) {
                255
            } else {
                127
            }
            addMarker(CircleMapMarker(it.coordinate, it.color, backgroundColor, opacity) {
                onBeaconClick(it)
            })
            if (it.icon != null) {
                val image = drawer.loadImage(it.icon.icon, size, size)
                addMarker(BitmapMapMarker(it.coordinate, image, size = 8f, tint = Color.WHITE) {
                    onBeaconClick(it)
                })
            }
        }
    }
}
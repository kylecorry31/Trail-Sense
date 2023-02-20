package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.navigation.ui.markers.CircleMapMarker

class BeaconLayer(
    private val size: Float = 12f,
    private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }
) :
    BaseLayer() {

    private val _beacons = mutableListOf<Beacon>()
    private var _highlighted: Beacon? = null

    private var _beaconUpToDate = false
    private var _loader: DrawerBitmapLoader? = null
    private var _imageSize = 8f

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    fun setBeacons(beacons: List<Beacon>) {
        _beacons.clear()
        _beacons.addAll(beacons)
        _beaconUpToDate = false
        updateMarkers()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (_loader == null) {
            _imageSize = drawer.dp(24f)
            _loader = DrawerBitmapLoader(drawer)
        }
        if (!_beaconUpToDate) {
            updateMarkers()
        }
        super.draw(drawer, map)
    }

    protected fun finalize() {
        _loader?.clear()
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
        val drawer = _loader ?: return
        val size = _imageSize.toInt()
        _beaconUpToDate = true
        clearMarkers()
        _beacons.forEach {
            val opacity = if (_highlighted == null || _highlighted?.id == it.id) {
                255
            } else {
                127
            }
            addMarker(
                CircleMapMarker(
                    it.coordinate,
                    it.color,
                    backgroundColor,
                    opacity,
                    this.size
                ) {
                    onBeaconClick(it)
                })
            if (it.icon != null) {
                val image = drawer.load(it.icon.icon, size)
                val color = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color)
                addMarker(
                    BitmapMapMarker(
                        it.coordinate,
                        image,
                        size = this.size * 0.75f,
                        tint = color
                    ) {
                        onBeaconClick(it)
                    })
            }
        }
    }
}
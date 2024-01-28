package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.MapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.TextMapMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BeaconLayer(
    private val size: Float = 12f,
    // TODO: Make labels configurable: Preferred position, clipping, outline, etc
    private val showLabels: Boolean = false,
    private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }
) :
    BaseLayer() {

    private val _beacons = mutableListOf<Beacon>()
    private var _highlighted: Beacon? = null

    private var _loader: DrawerBitmapLoader? = null
    private var _imageSize = 8f

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    private val lock = Any()

    private val runner = CoroutineQueueRunner()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun setBeacons(beacons: List<Beacon>) {
        synchronized(lock) {
            _beacons.clear()
            _beacons.addAll(beacons)
        }
        updateMarkers()
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (_loader == null) {
            _imageSize = drawer.dp(24f)
            _loader = DrawerBitmapLoader(drawer)
            updateMarkers()
        }
        super.draw(drawer, map)
    }

    protected fun finalize() {
        _loader?.clear()
        _loader = null
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
        scope.launch {
            runner.replace {
                synchronized(lock) {
                    val markers = convertToMarkers(_beacons)
                    clearMarkers()
                    for (marker in markers) {
                        addMarker(marker)
                    }
                }
            }
        }
    }

    private fun convertToMarkers(beacons: List<Beacon>): List<MapMarker> {
        val loader = _loader ?: return emptyList()
        val size = _imageSize.toInt()
        val markers = mutableListOf<MapMarker>()
        beacons.forEach {

            // Reduce the opacity if the beacon is not highlighted
            val opacity = if (_highlighted == null || _highlighted?.id == it.id) {
                255
            } else {
                127
            }

            // Create the marker background
            markers.add(
                CircleMapMarker(
                    it.coordinate,
                    it.color,
                    backgroundColor,
                    opacity,
                    this.size
                ) {
                    onBeaconClick(it)
                })

            // Create the icon for the marker
            if (it.icon != null && !showLabels) {
                val image = loader.load(it.icon.icon, size)
                val color =
                    Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color)
                markers.add(
                    BitmapMapMarker(
                        it.coordinate,
                        image,
                        size = this.size * 0.75f,
                        tint = color
                    ) {
                        onBeaconClick(it)
                    })
            }

            if (showLabels && it.name.isNotBlank()) {
                val color =
                    Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color)
                markers.add(
                    TextMapMarker(
                        it.coordinate,
                        it.name.trim(),
                        color,
                        size = this.size * 0.75f
                    ) {
                        onBeaconClick(it)
                    }
                )
            }
        }
        return markers
    }

}
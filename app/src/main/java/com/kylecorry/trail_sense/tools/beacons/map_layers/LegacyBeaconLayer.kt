package com.kylecorry.trail_sense.tools.beacons.map_layers

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonRenderer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LegacyBeaconLayer(
    private val size: Float = 12f,
    // TODO: Make labels configurable: Preferred position, clipping, outline, etc
    private val showLabels: Boolean = false,
    private val onBeaconClick: (beacon: Beacon) -> Boolean = { false }
) :
    IAsyncLayer {

    private val geoJsonRenderer = GeoJsonRenderer()

    private val _beacons = mutableListOf<Beacon>()
    private val _featureToBeacon = mutableMapOf<GeoJsonFeature, Beacon>()
    private var _highlighted: Beacon? = null

    @ColorInt
    private var backgroundColor = Color.TRANSPARENT

    private val lock = Any()

    private val runner = CoroutineQueueRunner()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        geoJsonRenderer.setOnClickListener {
            val beacon = synchronized(lock) {
                _featureToBeacon[it] ?: return@setOnClickListener false
            }
            onBeaconClick(beacon)
        }
        geoJsonRenderer.configurePointRenderer(shouldRenderLabels = showLabels)
    }

    fun setBeacons(beacons: List<Beacon>) {
        synchronized(lock) {
            _beacons.clear()
            _beacons.addAll(beacons)
        }
        updateMarkers()
    }

    fun setPreferences(prefs: BeaconMapLayerPreferences) {
        _percentOpacity = prefs.opacity.get() / 100f
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        geoJsonRenderer.draw(drawer, map)
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        geoJsonRenderer.invalidate()
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return geoJsonRenderer.onClick(drawer, map, pixel)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        geoJsonRenderer.setHasUpdateListener(listener)
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
                    _featureToBeacon.clear()
                    geoJsonRenderer.setGeoJsonObject(
                        GeoJsonFeatureCollection(
                            _beacons.map {
                                val point = GeoJsonFeature.point(
                                    it.coordinate,
                                    it.id,
                                    it.name.trim(),
                                    color = it.color,
                                    strokeColor = backgroundColor,
                                    opacity = if (_highlighted == null || _highlighted?.id == it.id) {
                                        255
                                    } else {
                                        127
                                    },
                                    size = size,
                                    icon = it.icon?.icon,
                                    iconSize = 12f * 0.75f,
                                    iconColor = Colors.mostContrastingColor(
                                        Color.WHITE,
                                        Color.BLACK,
                                        it.color
                                    ),
                                    isClickable = true
                                )
                                _featureToBeacon[point] = it
                                point
                            }
                        ))


                }
            }
        }
    }

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity

}
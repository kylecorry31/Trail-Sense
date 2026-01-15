package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonRenderer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class MapDistanceLayer : ILayer {

    var onPathChanged: (points: List<Coordinate>) -> Unit = {}
    private val renderer = GeoJsonRenderer()
    private var points = mutableListOf<Coordinate>()
    private var pathColor = Color.BLACK
    private var outlineColor = Color.WHITE

    init {
        renderer.setOnClickListener {
            val geometry = it.geometry
            if (geometry is GeoJsonPoint) {
                geometry.point?.coordinate?.let { point -> add(point) }
                true
            } else {
                false
            }
        }
    }

    var isEnabled = true
        set(value) {
            field = value
            clear()
        }

    fun add(location: Coordinate) {
        if (location == points.lastOrNull()) {
            return
        }
        points.add(location)
        onPathChanged(points.toList())
        updateLayers()
    }

    fun undo() {
        if (points.isNotEmpty()) {
            points.removeLastOrNull()
            onPathChanged(points.toList())
            updateLayers()
        }
    }

    fun clear() {
        points.clear()
        onPathChanged(points.toList())
        updateLayers()
    }

    fun getPoints(): List<Coordinate> {
        return points
    }

    override val layerId: String = LAYER_ID

    override fun setPreferences(preferences: Bundle) {
        // Do nothing
    }

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        if (!isEnabled) {
            return
        }
        renderer.draw(drawer, map)
    }

    override fun drawOverlay(
        context: Context,
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        renderer.invalidate()
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        if (!isEnabled) {
            return false
        }

        if (renderer.onClick(drawer, map, pixel)) {
            return true
        }

        add(map.toCoordinate(pixel))
        return true
    }

    private fun updateLayers() {
        val features = mutableListOf<GeoJsonFeature>()

        if (points.isNotEmpty()) {
            // Path
            if (points.size > 1) {
                features.add(
                    GeoJsonFeature.lineString(
                        points,
                        color = pathColor,
                        lineStyle = LineStyle.Solid
                    )
                )
            }

            // Points
            points.forEach {
                features.add(
                    GeoJsonFeature.point(
                        it,
                        color = pathColor,
                        strokeColor = outlineColor,
                        isClickable = true
                    )
                )
            }
        }

        renderer.setGeoJsonObject(GeoJsonFeatureCollection(features))
    }

    override var percentOpacity: Float = 1f

    companion object {
        const val LAYER_ID = "map_distance"
    }
}

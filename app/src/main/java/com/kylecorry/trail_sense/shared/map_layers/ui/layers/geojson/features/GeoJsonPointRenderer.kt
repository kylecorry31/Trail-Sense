package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.shared.canvas.InteractionUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_MARKER_SHAPE_CIRCLE
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getIcon
import com.kylecorry.trail_sense.shared.extensions.getIconColor
import com.kylecorry.trail_sense.shared.extensions.getIconSize
import com.kylecorry.trail_sense.shared.extensions.getMarkerShape
import com.kylecorry.trail_sense.shared.extensions.getOpacity
import com.kylecorry.trail_sense.shared.extensions.getSize
import com.kylecorry.trail_sense.shared.extensions.getStrokeColor
import com.kylecorry.trail_sense.shared.extensions.getStrokeWeight
import com.kylecorry.trail_sense.shared.extensions.isClickable
import com.kylecorry.trail_sense.shared.extensions.isSizeInDp
import com.kylecorry.trail_sense.shared.extensions.useScale
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.MapMarker

class GeoJsonPointRenderer : FeatureRenderer() {

    private var lastHeight = 0
    private var lastWidth = 0
    private var cachedBounds = Rectangle(0f, 0f, 0f, 0f)
    private var markers = listOf<MapMarker>()

    private var onClickListener: (feature: GeoJsonFeature) -> Boolean = { false }

    private var bitmapLoader: DrawerBitmapLoader? = null

    private var featuresChanged = false

    override fun setFeatures(features: List<GeoJsonFeature>) {
        featuresChanged = true
        super.setFeatures(features)
    }

    override fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features.filter { it.geometry is GeoJsonPoint }
    }

    fun setOnClickListener(listener: (feature: GeoJsonFeature) -> Boolean) {
        this.onClickListener = listener
    }

    // TODO: Instead of map markers, render the geojson directly
    private fun convertFeaturesToMarkers(drawer: ICanvasDrawer, features: List<GeoJsonFeature>) {
        if (bitmapLoader == null) {
            bitmapLoader = DrawerBitmapLoader(drawer)
        }

        if (featuresChanged) {
            val newMarkers = mutableListOf<MapMarker>()
            features.forEach { feature ->
                val point = (feature.geometry as GeoJsonPoint).point ?: return@forEach
                val shape = feature.getMarkerShape()
                val icon = feature.getIcon()
                val size = feature.getSize() ?: 12f
                val iconSize = feature.getIconSize() ?: size
                val isClickable = feature.isClickable()
                if (shape == GEO_JSON_PROPERTY_MARKER_SHAPE_CIRCLE || (shape == null && icon == null)) {
                    newMarkers.add(
                        CircleMapMarker(
                            point.coordinate,
                            feature.getColor() ?: Color.TRANSPARENT,
                            feature.getStrokeColor(),
                            feature.getOpacity(),
                            size,
                            feature.getStrokeWeight() ?: 0.5f,
                            feature.isSizeInDp(),
                            feature.useScale(),
                            if (isClickable) {
                                { onClickListener(feature) }
                            } else {
                                { false }
                            }
                        ))
                }

                if (icon != null) {
                    val bitmap = bitmapLoader!!.load(icon, (iconSize * 2).toInt())
                    newMarkers.add(
                        BitmapMapMarker(
                            point.coordinate,
                            bitmap,
                            iconSize,
                            rotation = null,
                            tint = feature.getIconColor(),
                            onClickFn = if (isClickable) {
                                { onClickListener(feature) }
                            } else {
                                { false }
                            }
                        )
                    )
                }
            }

            markers = newMarkers

            featuresChanged = false
            notifyListeners()
        }
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView,
        features: List<GeoJsonFeature>
    ) {
        convertFeaturesToMarkers(drawer, features)
        val bounds = getBounds(drawer)
        markers.forEach {
            val anchor = map.toPixel(it.location)
            if (bounds.contains(anchor.toVector2(bounds.top))) {
                it.draw(drawer, anchor, map.layerScale, map.mapAzimuth + map.mapRotation)
            }
        }
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        val points = markers.map {
            val anchor = map.toPixel(it.location)
            val radius = drawer.dp(it.size * map.layerScale) / 2f
            it to PixelCircle(anchor, radius)
        }

        val clicked = InteractionUtils.getClickedPoints(
            PixelCircle(pixel, drawer.dp(InteractionUtils.CLICK_SIZE_DP)),
            points
        )

        for (marker in clicked) {
            if (marker.first.onClick()) {
                return true
            }
        }

        return false
    }

    private fun getBounds(drawer: ICanvasDrawer): Rectangle {
        if (drawer.canvas.height != lastHeight || drawer.canvas.width != lastWidth) {
            lastHeight = drawer.canvas.height
            lastWidth = drawer.canvas.width
            // Rotating by map rotation wasn't working around 90/270 degrees - this is a workaround
            // It will just render slightly more of the path than needed, but never less (since 45 is when the area is at its largest)
            cachedBounds = drawer.getBounds(45f)
        }
        return cachedBounds
    }

    protected fun finalize() {
        bitmapLoader?.clear()
        bitmapLoader = null
    }

}
package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.shared.canvas.InteractionUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_MARKER_SHAPE_CIRCLE
import com.kylecorry.trail_sense.shared.extensions.getBitmap
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getIcon
import com.kylecorry.trail_sense.shared.extensions.getIconColor
import com.kylecorry.trail_sense.shared.extensions.getIconSize
import com.kylecorry.trail_sense.shared.extensions.getMarkerShape
import com.kylecorry.trail_sense.shared.extensions.getMoveWithUserLocation
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.extensions.getOpacity
import com.kylecorry.trail_sense.shared.extensions.getRotateWithUserAzimuth
import com.kylecorry.trail_sense.shared.extensions.getScaleToLocationAccuracy
import com.kylecorry.trail_sense.shared.extensions.getSize
import com.kylecorry.trail_sense.shared.extensions.getSizeUnit
import com.kylecorry.trail_sense.shared.extensions.getStrokeColor
import com.kylecorry.trail_sense.shared.extensions.getStrokeWeight
import com.kylecorry.trail_sense.shared.extensions.isClickable
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.SizeUnit
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.navigation.ui.DrawerBitmapLoader
import com.kylecorry.trail_sense.tools.navigation.ui.markers.BitmapMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.CircleMapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.MapMarker
import com.kylecorry.trail_sense.tools.navigation.ui.markers.TextMapMarker

class GeoJsonPointRenderer : FeatureRenderer() {

    private var lastHeight = 0
    private var lastWidth = 0
    private var cachedBounds = Rectangle(0f, 0f, 0f, 0f)
    private var markers = listOf<MapMarker>()
    private var showLabels = false

    private var onClickListener: (feature: GeoJsonFeature) -> Boolean = { false }

    private var bitmapLoader: DrawerBitmapLoader? = null

    private var featuresChanged = false
    private var selectedFeatureId: String? = null

    override fun setFeatures(features: List<GeoJsonFeature>) {
        featuresChanged = true
        super.setFeatures(features)
    }

    override fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features.filter { it.geometry is GeoJsonPoint }
    }

    fun setShouldRenderLabels(shouldRenderLabels: Boolean) {
        showLabels = shouldRenderLabels
    }

    fun setOnClickListener(listener: (feature: GeoJsonFeature) -> Boolean) {
        this.onClickListener = listener
    }

    fun setSelectedFeature(featureId: String?) {
        if (selectedFeatureId == featureId) {
            return
        }
        selectedFeatureId = featureId
        featuresChanged = true
        notifyListeners()
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
                val bitmap = feature.getBitmap()
                val beaconIcon = icon?.let { BeaconIcon.entries.withId(it) }
                val iconRes = beaconIcon?.icon
                val size = feature.getSize() ?: 12f
                val sizeUnit = feature.getSizeUnit()
                val iconSize = feature.getIconSize() ?: size
                val isClickable = feature.isClickable()
                val name = feature.getName()
                val rotateWithUserAzimuth = feature.getRotateWithUserAzimuth()
                val moveWithUserLocation = feature.getMoveWithUserLocation()
                val scaleToLocationAccuracy = feature.getScaleToLocationAccuracy()
                val isSelected =
                    selectedFeatureId != null && feature.id?.toString() == selectedFeatureId

                // Add highlight ring behind the marker when selected
                if (isSelected) {
                    val ringSize = maxOf(size, iconSize)
                    // Outer ring
                    newMarkers.add(
                        CircleMapMarker(
                            if (moveWithUserLocation) null else point.coordinate,
                            Color.TRANSPARENT,
                            Color.BLACK,
                            255,
                            ringSize + HIGHLIGHT_RING_SIZE,
                            HIGHLIGHT_OUTER_STROKE_WEIGHT,
                            if (scaleToLocationAccuracy) SizeUnit.Meters else sizeUnit,
                            sizeUnit != SizeUnit.Meters || scaleToLocationAccuracy,
                            scaleToLocationAccuracy
                        ) { false }
                    )

                    // Inner ring
                    newMarkers.add(
                        CircleMapMarker(
                            if (moveWithUserLocation) null else point.coordinate,
                            Color.TRANSPARENT,
                            Color.WHITE,
                            255,
                            ringSize + HIGHLIGHT_RING_SIZE,
                            HIGHLIGHT_INNER_STROKE_WEIGHT,
                            if (scaleToLocationAccuracy) SizeUnit.Meters else sizeUnit,
                            sizeUnit != SizeUnit.Meters || scaleToLocationAccuracy,
                            scaleToLocationAccuracy
                        ) { false }
                    )
                }

                if (shape == GEO_JSON_PROPERTY_MARKER_SHAPE_CIRCLE || (shape == null && iconRes == null && bitmap == null)) {
                    newMarkers.add(
                        CircleMapMarker(
                            if (moveWithUserLocation) null else point.coordinate,
                            feature.getColor() ?: Color.TRANSPARENT,
                            feature.getStrokeColor(),
                            feature.getOpacity(),
                            if (scaleToLocationAccuracy) 1f else size,
                            feature.getStrokeWeight() ?: 0.5f,
                            if (scaleToLocationAccuracy) SizeUnit.Meters else sizeUnit,
                            sizeUnit != SizeUnit.Meters || scaleToLocationAccuracy,
                            scaleToLocationAccuracy,
                            if (isClickable) {
                                { onClickListener(feature) }
                            } else {
                                { false }
                            }
                        ))
                }

                if (iconRes != null) {
                    val iconBitmap = bitmapLoader!!.load(iconRes, (iconSize * 2).toInt())
                    newMarkers.add(
                        BitmapMapMarker(
                            if (moveWithUserLocation) null else point.coordinate,
                            iconBitmap,
                            iconSize,
                            rotation = null,
                            rotateWithUser = rotateWithUserAzimuth,
                            tint = feature.getIconColor(),
                            onClickFn = if (isClickable) {
                                { onClickListener(feature) }
                            } else {
                                { false }
                            }
                        )
                    )
                } else if (bitmap != null) {
                    newMarkers.add(
                        BitmapMapMarker(
                            if (moveWithUserLocation) null else point.coordinate,
                            bitmap,
                            iconSize,
                            rotation = null,
                            rotateWithUser = rotateWithUserAzimuth,
                            tint = feature.getIconColor(),
                            onClickFn = if (isClickable) {
                                { onClickListener(feature) }
                            } else {
                                { false }
                            }
                        )
                    )
                }

                if (showLabels && !name.isNullOrBlank()) {
                    val color = Colors.mostContrastingColor(
                        Color.WHITE,
                        Color.BLACK,
                        feature.getColor() ?: Color.BLACK
                    )
                    newMarkers.add(
                        TextMapMarker(
                            point.coordinate,
                            name.trim(),
                            color,
                            size = size * 0.75f
                        ) {
                            onClickListener(feature)
                        }
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
            val anchor = map.toPixel(it.location ?: map.userLocation)
            if (bounds.contains(anchor.toVector2(bounds.top))) {
                it.draw(
                    drawer,
                    anchor,
                    if (it.scaleToLocationAccuracy) {
                        map.userLocationAccuracy?.meters()?.value ?: 0f
                    } else {
                        map.layerScale
                    },
                    (if (it.rotateWithUser) map.userAzimuth.value else map.mapAzimuth) + map.mapRotation,
                    map.metersPerPixel
                )
            }
        }
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        val points = markers.map {
            val anchor = map.toPixel(it.location ?: map.userLocation)
            val radius = it.calculateSizeInPixels(
                drawer,
                map.metersPerPixel,
                if (it.scaleToLocationAccuracy) {
                    map.userLocationAccuracy?.meters()?.value ?: 0f
                } else {
                    map.layerScale
                }
            ) / 2f
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

    companion object {
        private const val HIGHLIGHT_OUTER_STROKE_WEIGHT = 3f
        private const val HIGHLIGHT_INNER_STROKE_WEIGHT = 1.5f
        private const val HIGHLIGHT_RING_SIZE = 4f
    }

}
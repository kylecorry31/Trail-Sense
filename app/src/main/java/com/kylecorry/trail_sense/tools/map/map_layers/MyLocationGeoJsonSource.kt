package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.os.Bundle
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.sensors.SensorService

class MyLocationGeoJsonSource : GeoJsonSource {

    private var arrowBitmap: Bitmap? = null
    private var arrowBitmapSizeDp: Float? = null
    private var arrowBitmapStrokeWeight: Float? = null
    private val hasCompass = AppServiceRegistry.get<SensorService>().hasCompass()

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val preferences = params.getPreferences()
        val drawAccuracy = preferences.getBoolean(
            SHOW_ACCURACY,
            DEFAULT_SHOW_ACCURACY
        )
        val largeIndicator = preferences.getBoolean(
            LARGE_INDICATOR,
            DEFAULT_LARGE_INDICATOR
        )
        val userLocationSizeDp = if (largeIndicator) LARGE_SIZE_DP else DEFAULT_SIZE_DP
        val strokeWeight = if (largeIndicator) 4f else 2f
        val color = Resources.getPrimaryMarkerColor(context)
        val features = mutableListOf<GeoJsonFeature>()

        if (drawAccuracy) {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    color = color,
                    opacity = 25,
                    moveWithUserLocation = true,
                    scaleToLocationAccuracy = true
                )
            )
        }

        val isWidget = params.getBoolean(MapLayerParams.PARAM_IS_WIDGET, false)

        if (hasCompass && !isWidget) {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    bitmap = getArrowBitmap(context, color, userLocationSizeDp, strokeWeight),
                    size = userLocationSizeDp,
                    moveWithUserLocation = true,
                    rotateWithUserAzimuth = true
                )
            )
        } else {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    color = color,
                    strokeColor = Color.WHITE,
                    strokeWeight = strokeWeight,
                    size = userLocationSizeDp,
                    moveWithUserLocation = true
                )
            )
        }
        return GeoJsonFeatureCollection(features)
    }

    private fun getArrowBitmap(context: Context, color: Int, sizeDp: Float, strokeWeight: Float): Bitmap {
        if (arrowBitmap != null && arrowBitmapSizeDp == sizeDp && arrowBitmapStrokeWeight == strokeWeight) {
            return arrowBitmap!!
        }

        val size = Resources.dp(context, sizeDp).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val bitmapDrawer = CanvasDrawer(context, canvas)

        val path = Path()
        // Bottom left
        path.moveTo(size * 0.1f, size.toFloat())

        // Top
        path.lineTo(size / 2f, 0f)

        // Bottom right
        path.lineTo(size * 0.9f, size.toFloat())

        // Middle dip
        path.lineTo(size / 2f, size * 0.8f)

        path.close()
        bitmapDrawer.push()
        bitmapDrawer.fill(color)
        bitmapDrawer.stroke(Color.WHITE)
        bitmapDrawer.strokeWeight(strokeWeight)
        bitmapDrawer.path(path)
        bitmapDrawer.pop()

        arrowBitmap = bitmap
        arrowBitmapSizeDp = sizeDp
        arrowBitmapStrokeWeight = strokeWeight
        return bitmap
    }

    companion object {
        const val SOURCE_ID = "my_location"
        const val SHOW_ACCURACY = "show_accuracy"
        const val DEFAULT_SHOW_ACCURACY = true
        const val LARGE_INDICATOR = "large_indicator"
        const val DEFAULT_LARGE_INDICATOR = false
        private const val DEFAULT_SIZE_DP = 20f
        private const val LARGE_SIZE_DP = 28f
    }

}

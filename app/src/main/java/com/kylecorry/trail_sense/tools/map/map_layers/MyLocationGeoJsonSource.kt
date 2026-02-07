package com.kylecorry.trail_sense.tools.map.map_layers

import android.content.Context

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource

class MyLocationGeoJsonSource : GeoJsonSource {

    private var color: Int? = null
    private var accuracyFillColor: Int? = null
    private var drawAccuracy: Boolean = true
    private var showDirection: Boolean = false
    private var arrowBitmap: Bitmap? = null

    fun setStyle(
        color: Int,
        accuracyFillColor: Int,
        drawAccuracy: Boolean,
        showDirection: Boolean,
        arrowBitmap: Bitmap?
    ) {
        this.color = color
        this.accuracyFillColor = accuracyFillColor
        this.drawAccuracy = drawAccuracy
        this.showDirection = showDirection
        this.arrowBitmap = arrowBitmap
    }

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val features = mutableListOf<GeoJsonFeature>()

        if (drawAccuracy) {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    color = accuracyFillColor ?: Color.WHITE,
                    opacity = 25,
                    moveWithUserLocation = true,
                    scaleToLocationAccuracy = true
                )
            )
        }

        if (showDirection) {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    bitmap = arrowBitmap,
                    size = 16f,
                    moveWithUserLocation = true,
                    rotateWithUserAzimuth = true
                )
            )
        } else {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    color = color ?: Color.WHITE,
                    strokeColor = Color.WHITE,
                    strokeWeight = 2f,
                    size = 16f,
                    moveWithUserLocation = true
                )
            )
        }
        return GeoJsonFeatureCollection(features)
    }

    companion object {
        const val SOURCE_ID = "my_location"
        const val SHOW_ACCURACY = "show_accuracy"
        const val DEFAULT_SHOW_ACCURACY = true
    }

}

package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.graphics.Color
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_SIZE_UNIT_METERS
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_SIZE_UNIT_PIXELS
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel

class CellTowerGeoJsonSource : GeoJsonSource {

    private val minZoomLevel = 10

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject? {
        val zoomLevel = TileMath.distancePerPixelToZoom(
            metersPerPixel.toDouble(),
            (bounds.north + bounds.south) / 2
        )

        if (zoomLevel < minZoomLevel) {
            return null
        }

        val towers = CellTowerModel.getTowers(bounds)
        return GeoJsonFeatureCollection(
            towers.map {
                GeoJsonFeature.point(
                    it.coordinate,
                    color = Color.WHITE,
                    opacity = 25,
                    size = 2 * it.accuracy.meters().value,
                    sizeUnit = GEO_JSON_PROPERTY_SIZE_UNIT_METERS,
                    useScale = false,
                    icon = R.drawable.cell_tower,
                    iconColor = Color.WHITE,
                    iconSize = 12f,
                    isClickable = true,
                    additionalProperties = mapOf(
                        GEO_JSON_PROPERTY_ACCURACY to it.accuracy.meters().value
                    )
                )
            })
    }

    companion object {
        const val GEO_JSON_PROPERTY_ACCURACY = "accuracy"
    }
}
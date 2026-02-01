package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.graphics.Color
import android.os.Bundle
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.extensions.GEO_JSON_PROPERTY_SIZE_UNIT_METERS
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel

class CellTowerGeoJsonSource : GeoJsonSource {

    var featureName: String? = null

    override suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val towers = CellTowerModel.getTowers(bounds)
        return GeoJsonFeatureCollection(
            towers.map {
                GeoJsonFeature.point(
                    it.coordinate,
                    id = it.coordinate.toString(),
                    color = Color.WHITE,
                    opacity = 25,
                    size = 2 * it.accuracy.meters().value,
                    sizeUnit = GEO_JSON_PROPERTY_SIZE_UNIT_METERS,
                    icon = BeaconIcon.CellTower.id,
                    iconColor = Color.WHITE,
                    iconSize = 12f,
                    isClickable = true,
                    name = featureName,
                    layerId = CellTowerMapLayer.LAYER_ID,
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
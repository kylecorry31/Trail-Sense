package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.extensions.getFloatProperty
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.text.StringLoader
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class CellTowerMapLayer(
    private val onClick: (tower: ApproximateCoordinate) -> Boolean = { false }
) : GeoJsonLayer<CellTowerGeoJsonSource>(
    CellTowerGeoJsonSource(),
    minZoomLevel = 11,
    layerId = LAYER_ID
) {

    override fun onClick(feature: GeoJsonFeature): Boolean {
        val point = (feature.geometry as? GeoJsonPoint)?.point?.coordinate ?: return false
        val accuracy =
            feature.getFloatProperty(CellTowerGeoJsonSource.GEO_JSON_PROPERTY_ACCURACY) ?: 0f
        return onClick(
            ApproximateCoordinate(
                point.latitude,
                point.longitude,
                Distance.meters(accuracy)
            )
        )
    }

    companion object {

        const val LAYER_ID = "cell_tower"

        fun navigate(tower: ApproximateCoordinate) {
            val navigator = getAppService<Navigator>()
            val strings = getAppService<StringLoader>()
            navigator.navigateTo(
                tower.coordinate,
                strings.getString(R.string.cell_tower),
                BeaconOwner.Maps
            )
        }
    }
}
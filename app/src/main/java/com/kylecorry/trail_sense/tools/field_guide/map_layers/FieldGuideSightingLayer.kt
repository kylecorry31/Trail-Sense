package com.kylecorry.trail_sense.tools.field_guide.map_layers

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class FieldGuideSightingLayer : GeoJsonLayer<FieldGuideSightingGeoJsonSource>(
    FieldGuideSightingGeoJsonSource(),
    layerId = LAYER_ID
) {

    var onClick: (sighting: SightingDetails) -> Boolean = {
        navigate(it.location, it.name)
        true
    }

    override fun onClick(feature: GeoJsonFeature): Boolean {
        val sightingId = (feature.id as? Long?) ?: return false
        val pageId =
            feature.getLongProperty(FieldGuideSightingGeoJsonSource.GEO_JSON_PROPERTY_PAGE_ID)
                ?: return false
        val location = (feature.geometry as? GeoJsonPoint)?.point?.coordinate ?: return false
        val name = feature.getName() ?: ""
        return onClick(SightingDetails(pageId, sightingId, location, name))
    }

    private fun navigate(location: Coordinate, name: String) {
        val navigator = getAppService<Navigator>()

        navigator.navigateTo(
            location,
            name,
            BeaconOwner.Maps
        )
    }

    companion object {
        const val LAYER_ID = "field_guide_sighting"
    }
}

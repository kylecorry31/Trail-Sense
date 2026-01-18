package com.kylecorry.trail_sense.tools.field_guide.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class FieldGuideSightingLayer : GeoJsonLayer<FieldGuideSightingGeoJsonSource>(
    FieldGuideSightingGeoJsonSource(),
    layerId = LAYER_ID
) {

    companion object {
        const val LAYER_ID = "field_guide_sighting"
        const val PROPERTY_PAGE_ID = "pageId"
    }
}

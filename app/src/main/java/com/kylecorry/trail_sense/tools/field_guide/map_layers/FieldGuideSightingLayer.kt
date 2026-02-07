package com.kylecorry.trail_sense.tools.field_guide.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class FieldGuideSightingLayer : GeoJsonLayer<FieldGuideSightingGeoJsonSource>(
    FieldGuideSightingGeoJsonSource(),
    layerId = FieldGuideSightingGeoJsonSource.SOURCE_ID
)
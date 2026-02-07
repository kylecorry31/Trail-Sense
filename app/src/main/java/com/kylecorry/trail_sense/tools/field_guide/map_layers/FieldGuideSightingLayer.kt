package com.kylecorry.trail_sense.tools.field_guide.map_layers

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class FieldGuideSightingLayer : GeoJsonLayer<FieldGuideSightingGeoJsonSource>(
    FieldGuideSightingGeoJsonSource(),
    layerId = FieldGuideSightingGeoJsonSource.SOURCE_ID
) {

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        source.context = context
        super.draw(context, drawer, map)
    }

}

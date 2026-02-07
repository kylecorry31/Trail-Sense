package com.kylecorry.trail_sense.tools.field_guide.map_layers

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class FieldGuideSightingLayer : GeoJsonLayer<FieldGuideSightingGeoJsonSource>(
    FieldGuideSightingGeoJsonSource(),
    layerId = LAYER_ID
) {

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        source.context = context
        super.draw(context, drawer, map)
    }

    companion object {
        const val LAYER_ID = "field_guide_sighting"
        const val PROPERTY_PAGE_ID = "pageId"
        const val PREFERENCE_SHOW_IMAGES = "show_images"
    }
}

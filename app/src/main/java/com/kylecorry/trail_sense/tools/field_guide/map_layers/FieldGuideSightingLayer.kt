package com.kylecorry.trail_sense.tools.field_guide.map_layers

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class FieldGuideSightingLayer : GeoJsonLayer<FieldGuideSightingGeoJsonSource>(
    FieldGuideSightingGeoJsonSource(),
    layerId = LAYER_ID
) {

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        if (source.nameFormat.isEmpty()) {
            source.nameFormat = context.getString(R.string.sighting_label)
        }
        source.context = context
        super.draw(context, drawer, map)
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        source.showImages = preferences.getBoolean(PREFERENCE_SHOW_IMAGES, false)
    }

    companion object {
        const val LAYER_ID = "field_guide_sighting"
        const val PROPERTY_PAGE_ID = "pageId"
        const val PREFERENCE_SHOW_IMAGES = "show_images"
    }
}

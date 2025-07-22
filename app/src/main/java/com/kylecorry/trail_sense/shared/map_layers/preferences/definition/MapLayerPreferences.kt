package com.kylecorry.trail_sense.shared.map_layers.preferences.definition

import android.content.Context
import com.kylecorry.trail_sense.R

data class MapLayerPreferences(
    val layerId: String,
    val title: String,
    val preferences: List<MapLayerPreference>
) {
    companion object {
        // TODO: Extract to a map layer registration
        fun contours(context: Context, enabledByDefault: Boolean = false): MapLayerPreferences {
            return MapLayerPreferences(
                "contour_layer", context.getString(R.string.contours), listOf(
                    SwitchMapLayerPreference(
                        context.getString(R.string.visible),
                        "contour_layer_enabled",
                        defaultValue = enabledByDefault
                    ),
                    SeekbarMapLayerPreference(
                        context.getString(R.string.opacity),
                        "contour_layer_opacity",
                        defaultValue = 50,
                        dependency = "contour_layer_enabled"
                    ),
                    SwitchMapLayerPreference(
                        context.getString(R.string.show_labels),
                        "contour_layer_show_labels",
                        dependency = "contour_layer_enabled",
                        defaultValue = true
                    ),
                    SwitchMapLayerPreference(
                        context.getString(R.string.color_with_elevation),
                        "contour_layer_color_with_elevation",
                        defaultValue = false,
                        dependency = "contour_layer_enabled"
                    )
                )
            )
        }

        fun photoMaps(context: Context, enabledByDefault: Boolean = true, defaultOpacity: Int = 50): MapLayerPreferences {
            return MapLayerPreferences(
                "map_layer", context.getString(R.string.photo_maps), listOf(
                    SwitchMapLayerPreference(
                        context.getString(R.string.visible),
                        "map_layer_enabled",
                        defaultValue = enabledByDefault
                    ),
                    SeekbarMapLayerPreference(
                        context.getString(R.string.opacity),
                        "map_layer_opacity",
                        defaultValue = defaultOpacity,
                        dependency = "map_layer_enabled"
                    ),
                    SwitchMapLayerPreference(
                        context.getString(R.string.load_pdf_tiles),
                        "map_layer_load_pdfs",
                        defaultValue = false,
                        dependency = "map_layer_enabled",
                        summary = context.getString(R.string.load_pdf_tiles_summary)
                    ),
                )
            )
        }
    }
}
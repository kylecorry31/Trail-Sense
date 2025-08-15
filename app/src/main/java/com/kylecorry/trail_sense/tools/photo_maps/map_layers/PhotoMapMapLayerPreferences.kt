package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class PhotoMapMapLayerPreferences(
    context: Context,
    mapId: String,
    enabledByDefault: Boolean = true,
    defaultOpacity: Int = 50
) : BaseMapLayerPreferences(context) {
    private var _isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_map_layer_enabled",
        enabledByDefault
    )

    val isEnabled = MapLayerPreferenceConfig(
        get = { _isEnabled },
        set = { _isEnabled = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.visible),
            "map_layer_enabled",
            defaultValue = enabledByDefault
        )
    )

    private var _opacity by IntPreference(
        cache,
        "pref_${mapId}_map_layer_opacity",
        defaultOpacity // percent
    )

    val opacity = MapLayerPreferenceConfig(
        get = { _opacity },
        set = { _opacity = it },
        preference = SeekbarMapLayerPreference(
            context.getString(R.string.opacity),
            "map_layer_opacity",
            defaultValue = defaultOpacity,
            dependency = "map_layer_enabled"
        )
    )

    private var _loadPdfs by BooleanPreference(
        cache,
        "pref_${mapId}_map_layer_load_pdfs",
        false
    )

    val loadPdfs = MapLayerPreferenceConfig(
        get = { _loadPdfs },
        set = { _loadPdfs = it },
        preference = SwitchMapLayerPreference(
            context.getString(R.string.load_pdf_tiles),
            "map_layer_load_pdfs",
            defaultValue = false,
            dependency = "map_layer_enabled",
            summary = context.getString(R.string.load_pdf_tiles_summary)
        )
    )

    override fun getPreferences(): MapLayerViewPreferences {
        return MapLayerViewPreferences(
            "map_layer",
            context.getString(R.string.photo_maps),
            listOf(
                isEnabled.preference,
                opacity.preference,
                loadPdfs.preference
            )
        )
    }
}
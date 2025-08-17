package com.kylecorry.trail_sense.tools.photo_maps.map_layers

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class PhotoMapMapLayerPreferences(
    context: Context,
    mapId: String,
    enabledByDefault: Boolean = true,
    defaultOpacity: Int = 50
) : BaseMapLayerPreferences(
    context,
    mapId,
    "map",
    R.string.photo_maps,
    enabledByDefault,
    defaultOpacity
) {

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
            dependency = enabledPreferenceId,
            summary = context.getString(R.string.load_pdf_tiles_summary)
        )
    )

    override fun getAllPreferences(): List<MapLayerViewPreference> {
        return listOf(
            isEnabled.preference,
            opacity.preference,
            loadPdfs.preference
        )
    }
}
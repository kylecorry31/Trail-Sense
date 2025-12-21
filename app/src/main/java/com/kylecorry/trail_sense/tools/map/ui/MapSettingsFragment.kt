package com.kylecorry.trail_sense.tools.map.ui

import android.os.Bundle
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.tools.map.MapToolRegistration

class MapSettingsFragment : AndromedaPreferenceFragment() {

    private var layerSheet: MapLayersBottomSheet? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.map_preferences, rootKey)

        // Layers
        onClick(preference(R.string.pref_map_layer_button)) {
            layerSheet?.dismiss()
            layerSheet = MapLayersBottomSheet(
                MapToolRegistration.MAP_ID,
                MapToolLayerManager.orderedLayerIds
            )
            layerSheet?.show(this)
        }
    }

    override fun onPause() {
        super.onPause()
        layerSheet?.dismiss()
    }
}
package com.kylecorry.trail_sense.tools.navigation.ui.managers

import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class MapLayerSheetManager(
    private val fragment: Fragment,
    private val layers: NavigationCompassLayerManager
) {
    private var layerSheet: MapLayersBottomSheet? = null

    fun open(map: MapView) {
        layerSheet?.dismiss()
        layerSheet = MapLayersBottomSheet(
            NavigationToolRegistration.MAP_ID
        )
        layers.pause(map)
        layerSheet?.setOnDismissListener {
            layers.resume(fragment.requireContext(), map)
        }
        layerSheet?.show(fragment)
    }

    fun close() {
        layerSheet?.setOnDismissListener(null)
        layerSheet?.dismiss()
    }

}

package com.kylecorry.trail_sense.tools.navigation.ui.managers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class NavigationCompassLayerManager {
    private val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()

    var key = 0
        private set

    fun resume(context: Context, view: MapView) {
        view.setLayersWithPreferences(
            NavigationToolRegistration.MAP_ID,
            repo.getActiveLayerIds(NavigationToolRegistration.MAP_ID)
        )

        view.isPanEnabled = false
        view.isFlingEnabled = false
        view.backgroundColorOverride = Resources.color(context, R.color.colorSecondary)
        view.minScale = 0.001f
        view.metersPerProjectedUnit = 1.0
        view.latitudeScaleFactor = { 1f }

        key += 1
        view.start()
    }

    fun pause(view: IMapView) {
        view.stop()
    }
}

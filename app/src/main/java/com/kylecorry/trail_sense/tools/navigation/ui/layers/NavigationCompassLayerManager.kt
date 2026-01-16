package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class NavigationCompassLayerManager {
    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()

    var key = 0
        private set

    fun resume(context: Context, view: IMapView) {
        view.setLayersWithPreferences(
            NavigationToolRegistration.MAP_ID,
            repo.getActiveLayerIds(NavigationToolRegistration.MAP_ID)
        )

        key += 1

        if (prefs.navigation.useRadarCompass) {
            view.start()
        }
    }

    fun pause(view: IMapView) {
        view.stop()
    }
}
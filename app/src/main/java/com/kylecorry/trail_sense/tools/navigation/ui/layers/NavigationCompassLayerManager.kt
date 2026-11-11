package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.view.View
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.IMapLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class NavigationCompassLayerManager : IMapLayerManager {
    private val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()

    private var isRunning = false
    private val runningLock = Any()

    override fun resume(view: IMapView, fragment: Fragment, onLayersChanged: () -> Unit) {
        synchronized(runningLock) {
            isRunning = true
        }
        fragment.inBackground {
            view.setLayersWithPreferences(
                NavigationToolRegistration.MAP_ID,
                repo.getActiveLayerIds(NavigationToolRegistration.MAP_ID)
            )

            synchronized(runningLock) {
                if (isRunning) {
                    view.start()

                    if (view is View) {
                        view.invalidate()
                    }
                }
            }

            onLayersChanged()
        }
    }

    override fun pause(view: IMapView) {
        synchronized(runningLock) {
            isRunning = false
            view.stop()
        }
    }
}

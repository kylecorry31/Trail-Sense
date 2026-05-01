package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class NavigationCompassLayerManager {
    private val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()

    var key = 0
        private set

    private var isRunning = false
    private val runningLock = Any()

    fun resume(context: Context, view: IMapView, fragment: Fragment) {
        synchronized(runningLock) {
            isRunning = true
        }
        fragment.inBackground {
            view.setLayersWithPreferences(
                NavigationToolRegistration.MAP_ID,
                repo.getActiveLayerIds(NavigationToolRegistration.MAP_ID)
            )

            key += 1

            synchronized(runningLock) {
                if (isRunning) {
                    view.start()

                    if (view is View) {
                        view.invalidate()
                    }
                }
            }
        }
    }

    fun pause(view: IMapView) {
        synchronized(runningLock) {
            isRunning = false
            view.stop()
        }
    }
}

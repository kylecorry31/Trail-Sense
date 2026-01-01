package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.map.map_layers.BackgroundColorMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer

class NavigationCompassLayerManager {
    private val taskRunner = MapLayerBackgroundTask()
    private val prefs = AppServiceRegistry.get<UserPreferences>()

    var key = 0
        private set

    fun resume(context: Context, view: IMapView) {
        view.setLayersWithPreferences(
            NavigationToolRegistration.MAP_ID,
            defaultLayers,
            taskRunner
        )

        key += 1

        if (prefs.navigation.useRadarCompass) {
            view.start()
        }
    }

    fun pause(view: IMapView) {
        view.stop()
    }

    companion object {
        val defaultLayers = listOf(
            BackgroundColorMapLayer.LAYER_ID,
            BaseMapLayer.LAYER_ID,
            ElevationLayer.LAYER_ID,
            HillshadeLayer.LAYER_ID,
            PhotoMapLayer.LAYER_ID,
            ContourLayer.LAYER_ID,
            NavigationLayer.LAYER_ID,
            CellTowerMapLayer.LAYER_ID,
            TideMapLayer.LAYER_ID,
            PathLayer.LAYER_ID,
            BeaconLayer.LAYER_ID,
            MyLocationLayer.LAYER_ID,
        )
    }
}
package com.kylecorry.trail_sense.shared.maps.picker

import android.graphics.Color
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.map.MapToolRegistration

class MapLocationPickerViewManager {
    private val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()
    private val selectedPointLayer = ConfigurableGeoJsonLayer()
    private var isRunning = false
    private val runningLock = Any()

    fun start(view: IMapView, owner: LifecycleOwner) {
        synchronized(runningLock) {
            isRunning = true
        }
        owner.inBackground {
            view.setLayersWithPreferences(
                MapToolRegistration.MAP_ID,
                repo.getActiveLayerIds(MapToolRegistration.MAP_ID),
                listOf(selectedPointLayer)
            )

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

    fun stop(view: IMapView) {
        synchronized(runningLock) {
            isRunning = false
            view.stop()
        }
    }

    fun setSelectedLocation(location: Coordinate?) {
        if (location == null) {
            selectedPointLayer.setData(GeoJsonFeatureCollection(emptyList()))
        } else {
            val point = GeoJsonFeature.point(
                location,
                strokeColor = Color.WHITE,
                color = Color.BLACK
            )
            selectedPointLayer.setData(point)
        }
    }
}

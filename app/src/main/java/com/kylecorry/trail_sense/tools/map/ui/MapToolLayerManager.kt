package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.sharing.GeoJsonFeatureClickHandler
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.photo_maps.ui.MapDistanceLayer

class MapToolLayerManager {
    private val selectedPointLayer = ConfigurableGeoJsonLayer()
    private val distanceLayer = MapDistanceLayer()
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null
    private val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()

    var key: Int = 0

    fun resume(context: Context, view: IMapView, fragment: Fragment) {
        view.setLayersWithPreferences(
            MapToolRegistration.MAP_ID,
            repo.getActiveLayerIds(MapToolRegistration.MAP_ID) +
                    listOf(
                        ScaleBarLayer.LAYER_ID,
                        MyElevationLayer.LAYER_ID,
                        CompassOverlayLayer.LAYER_ID,
                    ),
            listOf(

                selectedPointLayer,
                distanceLayer
            )
        )

        // Hardcoded configuration
        distanceLayer.onPathChanged = { onDistancePathChange(it) }
        distanceLayer.isEnabled = false
        view.getLayer<CompassOverlayLayer>()?.paddingTopDp = 48f

        view.layerManager.setOnGeoJsonFeatureClickListener { feature ->
            GeoJsonFeatureClickHandler.handleFeatureClick(fragment, feature)
        }

        view.start()

        if (view is View) {
            view.invalidate()
        }

        key++
    }

    fun pause(view: IMapView) {
        view.stop()
    }

    fun onBoundsChanged() {
        distanceLayer.invalidate()
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

    // Distance measurement

    private fun onDistancePathChange(points: List<Coordinate>) {
        // Display distance
        val distance = Geology.getPathDistance(points)
        onDistanceChangedCallback?.invoke(distance)
    }

    fun setOnDistanceChangedCallback(callback: ((Distance) -> Unit)?) {
        onDistanceChangedCallback = callback
    }

    fun stopDistanceMeasurement() {
        distanceLayer.isEnabled = false
        distanceLayer.clear()
    }

    fun undoLastDistanceMeasurement() {
        distanceLayer.undo()
    }

    fun getDistanceMeasurementPoints(): List<Coordinate> {
        return distanceLayer.getPoints()
    }

    fun startDistanceMeasurement(vararg initialPoints: Coordinate) {
        distanceLayer.isEnabled = true
        distanceLayer.clear()
        initialPoints.forEach { distanceLayer.add(it) }
    }

    fun isMeasuringDistance(): Boolean {
        return distanceLayer.isEnabled
    }
}
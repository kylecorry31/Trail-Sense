package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.dem.map_layers.AspectLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.SlopeLayer
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.ui.MapDistanceLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer

class MapToolLayerManager {
    private val selectedPointLayer = ConfigurableGeoJsonLayer()
    private val distanceLayer = MapDistanceLayer()
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null

    var key: Int = 0

    fun resume(context: Context, view: IMapView) {
        view.setLayersWithPreferences(
            MapToolRegistration.MAP_ID,
            defaultLayers,
            // TODO: Extract these to layer config
            listOf(
                selectedPointLayer,
                distanceLayer
            )
        )

        // Hardcoded configuration
        distanceLayer.onPathChanged = { onDistancePathChange(it) }
        distanceLayer.isEnabled = false
        view.getLayer<CompassOverlayLayer>()?.paddingTopDp = 48f
        view.getLayer<CellTowerMapLayer>()?.onClick = {
            CellTowerMapLayer.navigate(it)
            true
        }
        view.getLayer<BeaconLayer>()?.onClick = {
            val navigator = AppServiceRegistry.get<Navigator>()
            navigator.navigateTo(it)
            true
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

    companion object {

        val defaultLayers = listOf(
            BaseMapLayer.LAYER_ID,
            ElevationLayer.LAYER_ID,
            HillshadeLayer.LAYER_ID,
            AspectLayer.LAYER_ID,
            SlopeLayer.LAYER_ID,
            PhotoMapLayer.LAYER_ID,
            ContourLayer.LAYER_ID,
            NavigationLayer.LAYER_ID,
            CellTowerMapLayer.LAYER_ID,
            TideMapLayer.LAYER_ID,
            PathLayer.LAYER_ID,
            BeaconLayer.LAYER_ID,
            MyLocationLayer.LAYER_ID,
            // Overlays
            ScaleBarLayer.LAYER_ID,
            MyElevationLayer.LAYER_ID,
            CompassOverlayLayer.LAYER_ID
        )

    }
}
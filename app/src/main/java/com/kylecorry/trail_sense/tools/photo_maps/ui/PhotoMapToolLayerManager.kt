package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.SlopeLayer
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.PhotoMapsToolRegistration
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer

class PhotoMapToolLayerManager {

    private var onBeaconClick: ((Beacon) -> Unit)? = null
    private val selectedPointLayer = ConfigurableGeoJsonLayer()
    private val distanceLayer = MapDistanceLayer()
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null
    private var photoMapLayer: PhotoMapLayer? = null

    private val preferences = AppServiceRegistry.get<PreferencesSubsystem>()

    private var lastMapDetails: Pair<CoordinateBounds, Float>? = null

    var key: Int = 0
        private set

    fun resume(context: Context, view: IMapView, photoMapId: Long) {
        // User can't disable the photo maps layer
        preferences.preferences.putBoolean("pref_photo_maps_map_layer_enabled", true)

        view.setLayersWithPreferences(
            PhotoMapsToolRegistration.MAP_ID,
            defaultLayers,
            // TODO: Extract these to layer config
            listOf(
                selectedPointLayer,
                distanceLayer
            )
        )

        key++

        // Hardcoded customization for this tool
        distanceLayer.isEnabled = false
        distanceLayer.onPathChanged = { onDistancePathChange(it) }
        lastMapDetails?.let { improveResolution(it.first, it.second) }


        photoMapLayer = view.getLayer<PhotoMapLayer>()
        photoMapLayer?.setPhotoMapFilter { it.id == photoMapId }
        photoMapLayer?.setMinZoomLevel(0)
        view.getLayer<BeaconLayer>()?.onClick = {
            onBeaconClick?.invoke(it)
            true
        }
        view.getLayer<CellTowerMapLayer>()?.onClick = {
            CellTowerMapLayer.navigate(it)
            true
        }

        view.start()
    }

    fun pause(view: IMapView) {
        view.stop()
    }

    fun onBoundsChanged() {
        distanceLayer.invalidate()
    }

    fun improveResolution(bounds: CoordinateBounds, metersPerPixel: Float) {
        lastMapDetails = bounds to metersPerPixel
        photoMapLayer?.improveResolution(bounds, metersPerPixel, 70)
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
            selectedPointLayer.setData(GeoJsonFeatureCollection(listOf(point)))
        }
    }

    fun setOnBeaconClickListener(listener: ((Beacon) -> Unit)?) {
        onBeaconClick = listener
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
        val alwaysEnabledLayers = listOf(
            PhotoMapLayer.LAYER_ID
        )

        val defaultLayers = listOf(
            BaseMapLayer.LAYER_ID,
            ElevationLayer.LAYER_ID,
            HillshadeLayer.LAYER_ID,
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
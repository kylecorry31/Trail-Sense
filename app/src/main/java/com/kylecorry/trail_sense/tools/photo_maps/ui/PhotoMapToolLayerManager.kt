package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getLayerById
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sharing.GeoJsonFeatureClickHandler
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.photo_maps.PhotoMapsToolRegistration
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.PhotoMapPreferences
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapTileSource

class PhotoMapToolLayerManager {

    private val selectedPointLayer = ConfigurableGeoJsonLayer()
    private val distanceLayer = MapDistanceLayer()
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null
    private var photoMapLayer: TileMapLayer<*>? = null

    private val preferences = AppServiceRegistry.get<PreferencesSubsystem>()
    private val photoMapPreferences = PhotoMapPreferences(AppServiceRegistry.get())
    private val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()

    private var lastMapDetails: Pair<CoordinateBounds, Int>? = null

    var key: Int = 0
        private set

    fun resume(context: Context, view: IMapView, photoMapId: Long, fragment: Fragment) {
        // User can't disable the photo maps layer
        preferences.preferences.putBoolean("pref_photo_maps_map_layer_enabled", true)

        view.setLayersWithPreferences(
            PhotoMapsToolRegistration.MAP_ID,
            repo.getActiveLayerIds(PhotoMapsToolRegistration.MAP_ID) + listOf(
                ScaleBarLayer.LAYER_ID,
                MyElevationLayer.LAYER_ID,
                CompassOverlayLayer.LAYER_ID
            ),
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


        photoMapLayer = view.getLayerById(PhotoMapTileSource.SOURCE_ID) as? TileMapLayer<*>
        photoMapLayer?.setFeatureFilter(photoMapId.toString())
        photoMapLayer?.setMinZoomLevel(0)

        view.layerManager.setOnGeoJsonFeatureClickListener { feature ->
            GeoJsonFeatureClickHandler.handleFeatureClick(fragment, feature)
        }

        view.start()
    }

    fun pause(view: IMapView) {
        view.stop()
    }

    fun onBoundsChanged() {
        distanceLayer.invalidate()
    }

    fun improveResolution(bounds: CoordinateBounds, zoom: Int) {
        lastMapDetails = bounds to zoom
        if (photoMapPreferences.highDetailMode) {
            photoMapLayer?.improveResolution(bounds, zoom, 70)
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
            selectedPointLayer.setData(GeoJsonFeatureCollection(listOf(point)))
        }
    }

    // Distance measurement

    private fun onDistancePathChange(points: List<Coordinate>) {
        // Display distance
        val distance = Geography.getPathDistance(points)
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
            PhotoMapTileSource.SOURCE_ID
        )

    }
}

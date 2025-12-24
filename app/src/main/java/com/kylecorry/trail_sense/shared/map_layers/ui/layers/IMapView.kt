package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.MapLayerRegistry
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getPreferenceValues

interface IMapView {
    fun addLayer(layer: ILayer)
    fun removeLayer(layer: ILayer)
    fun setLayers(layers: List<ILayer>)
    fun getLayers(): List<ILayer>

    fun start()
    fun stop()

    /**
     * The current projection of the map. The response should be fixed, so it doesn't change on consumers using it.
     */
    val mapProjection: IMapViewProjection

    var userLocation: Coordinate

    var userLocationAccuracy: Distance?

    var userAzimuth: Bearing

    /**
     * The scale in meters per pixel
     */
    var metersPerPixel: Float

    /**
     * The scale for each layer element
     * This is likely to be temporary until meters per pixel can be used
     */
    val layerScale: Float

    /**
     * The center of the map
     */
    var mapCenter: Coordinate

    /**
     * The azimuth of the MapView
     */
    var mapAzimuth: Float

    /**
     * The rotation of the map from azimuth. Ex. the top of the map doesn't point north
     */
    val mapRotation: Float

    val mapBounds: CoordinateBounds
}

fun IMapView.toPixel(coordinate: Coordinate): PixelCoordinate {
    return mapProjection.toPixels(coordinate)
}

fun IMapView.toPixel(latitude: Double, longitude: Double): PixelCoordinate {
    return mapProjection.toPixels(latitude, longitude)
}

fun IMapView.toCoordinate(pixel: PixelCoordinate): Coordinate {
    return mapProjection.toCoordinate(pixel)
}

fun IMapView.setLayersWithPreferences(
    context: Context,
    mapId: String,
    layerIds: List<String>,
    taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    additionalLayers: List<ILayer> = emptyList(),
    forceReplaceLayers: Boolean = false
) {
    val registry = AppServiceRegistry.get<MapLayerRegistry>()
    val layerDefinitions = registry.getLayers()
    val currentLayers = getLayers()
    val newLayerIds = layerIds + additionalLayers.map { it.layerId }
    val layers = if (!forceReplaceLayers && currentLayers.map { it.layerId } == newLayerIds) {
        currentLayers
    } else {
        layerIds.mapNotNull { id ->
            layerDefinitions.firstOrNull { it.id == id }?.create(context, taskRunner)
        } + additionalLayers
    }
    val layersToPreference = layers.map { layer ->
        layer to layerDefinitions.firstOrNull { it.id == layer.layerId }
            ?.getPreferenceValues(context, mapId)
    }
    val actualLayers =
        layersToPreference.filter {
            it.second?.getBoolean(
                DefaultMapLayerDefinitions.ENABLED
            ) != false
        }
    actualLayers.forEach {
        it.second?.let { prefs ->
            it.first.setPreferences(prefs)
            it.first.invalidate()
        }
    }

    setLayers(actualLayers.map { it.first })
}


@Suppress("UNCHECKED_CAST")
inline fun <reified T : ILayer> IMapView.getLayer(): T? {
    return getLayers().firstOrNull { it is T } as T?
}
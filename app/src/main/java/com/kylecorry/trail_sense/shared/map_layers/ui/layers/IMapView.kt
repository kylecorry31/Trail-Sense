package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.getAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo

interface IMapView {

    val layerManager: MapViewLayerManager

    /**
     * The current projection of the map. The response should be fixed, so it doesn't change on consumers using it.
     */
    val mapProjection: IMapViewProjection

    var userLocation: Coordinate

    var userLocationAccuracy: Distance?

    var userAzimuth: Bearing

    /**
     * The resolution in meters per pixel
     */
    var resolutionPixels: Float

    /**
     * Zoom level using resolution
     */
    val zoom: Float

    /**
     * The resolution in meters per density pixel
     */
    var resolution: Float

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

    /**
     * Set the feature click listener for the map
     */
    fun setOnGeoJsonFeatureClickListener(listener: ((GeoJsonFeature) -> Unit)?)
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

fun IMapView.setLayers(layers: List<ILayer>) {
    layerManager.setLayers(layers)
}

fun IMapView.start() {
    layerManager.start()
}

fun IMapView.stop() {
    layerManager.stop()
}

fun IMapView.setLayersWithPreferences(
    mapId: String,
    layerIds: List<String>,
    additionalLayers: List<ILayer> = emptyList(),
    forceReplaceLayers: Boolean = false
) {
    val loader = AppServiceRegistry.get<MapLayerLoader>()
    val repo = AppServiceRegistry.get<MapLayerPreferenceRepo>()
    val currentLayers = layerManager.getLayers()
    val newLayerIds = layerIds + additionalLayers.map { it.layerId }
    val layers = if (!forceReplaceLayers && currentLayers.map { it.layerId } == newLayerIds) {
        currentLayers
    } else {
        layerIds.mapNotNull { id ->
            loader.getLayer(id)
        } + additionalLayers
    }

    val layerPreferences = repo.getLayerPreferencesBundle(mapId, newLayerIds)

    val layersToPreference = layers.map { layer ->
        layer to layerPreferences[layer.layerId]
    }
    val actualLayers =
        layersToPreference.filter {
            it.second?.containsKey(DefaultMapLayerDefinitions.ENABLED) == false ||
                    it.second?.getBoolean(DefaultMapLayerDefinitions.ENABLED) != false
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
    return layerManager.getLayers().firstOrNull { it is T } as T?
}

suspend fun IMapView.getAttribution(context: Context): CharSequence? {
    val loader = AppServiceRegistry.get<MapLayerLoader>()
    return loader.getAttribution(context, layerManager.getLayers().map { it.layerId })
}
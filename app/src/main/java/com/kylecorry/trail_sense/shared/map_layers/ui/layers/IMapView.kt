package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.ENABLED
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getPreferenceValues
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

interface IMapView {
    fun addLayer(layer: ILayer)
    fun removeLayer(layer: ILayer)
    fun setLayers(layers: List<ILayer>)

    fun start()
    fun stop()

    /**
     * The current projection of the map. The response should be fixed, so it doesn't change on consumers using it.
     */
    val mapProjection: IMapViewProjection

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

fun IMapView.setLayersWithPreferences(vararg layers: Pair<ILayer?, BaseMapLayerPreferences?>) {
    val actualLayers = layers.filter { it.first != null && it.second?.isEnabled?.get() != false }
    actualLayers.forEach {
        it.second?.toBundle()?.let { prefs ->
            it.first?.setPreferences(prefs)
            it.first?.invalidate()
        }
    }

    setLayers(actualLayers.mapNotNull { it.first })
}

fun IMapView.setLayersWithPreferences2(context: Context, mapId: String, vararg layers: ILayer?) {
    val layerDefinitions = Tools.getTools(context).flatMap { it.mapLayers }
    val layersToPreference = layers.map { layer ->
        layer to layerDefinitions.firstOrNull { it.id == layer?.layerId }
            ?.getPreferenceValues(context, mapId)
    }
    val actualLayers =
        layersToPreference.filter { it.first != null && it.second?.getBoolean(ENABLED) != false }
    actualLayers.forEach {
        it.second?.let { prefs ->
            it.first?.setPreferences(prefs)
            it.first?.invalidate()
        }
    }

    setLayers(actualLayers.mapNotNull { it.first })
}

fun IMapView.setLayersWithPreferences(context: Context, mapId: String, vararg layers: ILayer?) {
    val preferences = Tools.getMap(context, mapId)?.layerPreferences ?: return

    val layersToPreference = layers.map { layer ->
        layer to preferences.firstOrNull { it.layerId == layer?.layerId }
    }

    setLayersWithPreferences(*layersToPreference.toTypedArray())
}

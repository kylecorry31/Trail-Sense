package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate

interface IMapView {
    fun addLayer(layer: ILayer)
    fun removeLayer(layer: ILayer)
    fun setLayers(layers: List<ILayer>)

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

fun IMapView.lineToPixels(
    coordinates: List<Coordinate>,
    line: FloatArray = FloatArray(coordinates.size * 4)
): FloatArray {
    if (coordinates.isEmpty()) {
        return line
    }
    var lastPixel = toPixel(coordinates[0])
    for (i in 1..coordinates.lastIndex) {
        val nextPixel = toPixel(coordinates[i])
        line[(i - 1) * 4] = lastPixel.x
        line[(i - 1) * 4 + 1] = lastPixel.y
        line[(i - 1) * 4 + 2] = nextPixel.x
        line[(i - 1) * 4 + 3] = nextPixel.y
        lastPixel = nextPixel
    }
    return line
}
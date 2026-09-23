package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Matrix
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlin.math.sqrt

internal class GeoJsonRenderTransform {
    val matrix = Matrix()
    var scale = 1f
        private set
    private val destination = FloatArray(8)
    private val values = FloatArray(9)
    private var lastProjection: IMapViewProjection? = null
    private var lastBounds: CoordinateBounds? = null
    private var lastCorners: FloatArray? = null

    fun update(projection: IMapViewProjection, bounds: CoordinateBounds, corners: FloatArray) {
        if (projection === lastProjection && bounds == lastBounds && corners === lastCorners) {
            return
        }
        val nw = projection.toPixels(bounds.northWest)
        val ne = projection.toPixels(bounds.northEast)
        val se = projection.toPixels(bounds.southEast)
        val sw = projection.toPixels(bounds.southWest)
        destination[0] = nw.x
        destination[1] = nw.y
        destination[2] = ne.x
        destination[3] = ne.y
        destination[4] = se.x
        destination[5] = se.y
        destination[6] = sw.x
        destination[7] = sw.y
        matrix.reset()
        matrix.setPolyToPoly(corners, 0, destination, 0, 4)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val skewY = values[Matrix.MSKEW_Y]
        scale = sqrt(scaleX * scaleX + skewY * skewY)
        lastProjection = projection
        lastBounds = bounds
        lastCorners = corners
    }
}

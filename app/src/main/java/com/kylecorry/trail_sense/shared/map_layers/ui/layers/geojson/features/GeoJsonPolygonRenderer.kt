package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.withMatrix
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPolygon
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.canvas.PolygonClipper
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getOpacity
import com.kylecorry.trail_sense.shared.extensions.getStrokeColor
import com.kylecorry.trail_sense.shared.extensions.getStrokeWeight
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import kotlin.math.absoluteValue

class GeoJsonPolygonRenderer : FeatureRenderer() {

    private var filterEpsilon = 0f
    private val clipper = PolygonClipper()
    private var pathPool = ObjectPool { Path() }
    private var polygons = listOf<PrecomputedPolygon>()
    private val lock = Any()
    private var updateListener: (() -> Unit)? = null
    private val matrix = Matrix()
    private val src = FloatArray(8)
    private val dst = FloatArray(8)

    init {
        setRunInBackgroundWhenChanged(this::renderFeaturesInBackground)
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        super.setHasUpdateListener(listener)
        updateListener = listener
    }

    override fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features.filter { it.geometry is GeoJsonPolygon }
    }

    private suspend fun renderFeaturesInBackground(
        viewBounds: Rectangle,
        bounds: CoordinateBounds,
        projection: IMapViewProjection,
        features: List<GeoJsonFeature>
    ) = onDefault {
        val rdp =
            RDPFilter<GeoJsonPosition>(projection.resolutionPixels.coerceAtLeast(1f) * filterEpsilon) { point, start, end ->
                Geology.getCrossTrackDistance(
                    point.coordinate,
                    start.coordinate,
                    end.coordinate
                ).value.absoluteValue
            }

        // Calculate the projected corners of the reference bounds
        val projectedNW = projection.toPixels(bounds.northWest)
        val projectedNE = projection.toPixels(bounds.northEast)
        val projectedSE = projection.toPixels(bounds.southEast)
        val projectedSW = projection.toPixels(bounds.southWest)

        val projectedCorners = floatArrayOf(
            projectedNW.x, projectedNW.y,
            projectedNE.x, projectedNE.y,
            projectedSE.x, projectedSE.y,
            projectedSW.x, projectedSW.y
        )

        val precomputed = features.mapNotNull { feature ->
            val geometry = feature.geometry as GeoJsonPolygon
            val rings = geometry.polygon ?: return@mapNotNull null

            val geometryBounds = geometry.boundingBox?.bounds ?: CoordinateBounds.from(
                rings.flatten().map { it.coordinate })
            if (!geometryBounds.intersects(bounds)) {
                return@mapNotNull null
            }

            val filteredRings = rings.map { ring ->
                rdp.filter(ring).map { it.coordinate }
            }

            PrecomputedPolygon(
                feature,
                filteredRings,
                feature.getColor(),
                feature.getStrokeColor(),
                feature.getStrokeWeight() ?: 0f,
                feature.getOpacity(),
                pathPool.get(),
                bounds,
                projectedCorners
            )
        }

        // Canvas bounds are inverted
        val margin = 100f
        val actualViewBounds = Rectangle(
            viewBounds.left - margin,
            viewBounds.bottom - margin,
            viewBounds.right + margin,
            viewBounds.top + margin
        )
        for (polygon in precomputed) {
            render(polygon, actualViewBounds, projection)
        }

        synchronized(lock) {
            polygons.forEach {
                pathPool.release(it.path)
            }
            polygons = precomputed
        }
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView,
        features: List<GeoJsonFeature>
    ) {
        if (filterEpsilon == 0f) {
            filterEpsilon = drawer.dp(2f)
        }

        val scale = map.layerScale
        drawer.noPathEffect()

        synchronized(lock) {
            for (polygon in polygons) {
                val path = polygon.path

                val currentNW = map.toPixel(polygon.referenceBounds.northWest)
                val currentNE = map.toPixel(polygon.referenceBounds.northEast)
                val currentSE = map.toPixel(polygon.referenceBounds.southEast)
                val currentSW = map.toPixel(polygon.referenceBounds.southWest)

                // Source points (precomputed projected corners)
                System.arraycopy(polygon.projectedCorners, 0, src, 0, 8)

                // Destination points (current screen coordinates)
                // NW
                dst[0] = currentNW.x
                dst[1] = currentNW.y
                // NE
                dst[2] = currentNE.x
                dst[3] = currentNE.y
                // SE
                dst[4] = currentSE.x
                dst[5] = currentSE.y
                // SW
                dst[6] = currentSW.x
                dst[7] = currentSW.y

                matrix.setPolyToPoly(src, 0, dst, 0, 4)

                // Calculate scale from matrix
                val matrixValues = FloatArray(9)
                matrix.getValues(matrixValues)
                val scaleX = matrixValues[Matrix.MSCALE_X]
                val skewY = matrixValues[Matrix.MSKEW_Y]
                val relativeScale = kotlin.math.sqrt(scaleX * scaleX + skewY * skewY)

                drawer.canvas.withMatrix(matrix) {

                    if (polygon.fillColor != null) {
                        drawer.fill(polygon.fillColor.withAlpha(polygon.opacity))
                    } else {
                        drawer.noFill()
                    }

                    if (polygon.strokeColor != null && polygon.strokeWeight > 0) {
                        drawer.stroke(polygon.strokeColor.withAlpha(polygon.opacity))
                        drawer.strokeWeight(drawer.dp(polygon.strokeWeight * scale) / relativeScale)
                        drawer.strokeJoin(StrokeJoin.Round)
                        drawer.strokeCap(StrokeCap.Round)
                    } else {
                        drawer.noStroke()
                    }

                    drawer.path(path)
                }
            }
        }

        drawer.noStroke()
        drawer.fill(Color.WHITE)
    }

    private fun render(
        polygon: PrecomputedPolygon,
        bounds: Rectangle,
        projection: IMapViewProjection
    ) {
        polygon.path.reset()
        polygon.path.fillType = Path.FillType.EVEN_ODD

        polygon.rings.forEach { ring ->
            if (ring.isEmpty()) {
                return@forEach
            }

            // Convert to pixels
            val pixels = ring.map { projection.toPixels(it) }

            // Clip to bounds
            val clipped = clipper.clip(pixels, bounds)
            if (clipped.isEmpty()) {
                return@forEach
            }

            val start = clipped[0]
            polygon.path.moveTo(start.x, start.y)

            for (i in 1 until clipped.size) {
                val pixel = clipped[i]
                polygon.path.lineTo(pixel.x, pixel.y)
            }
            polygon.path.close()
        }
    }

    class PrecomputedPolygon(
        val feature: GeoJsonFeature,
        val rings: List<List<Coordinate>>,
        val fillColor: Int?,
        val strokeColor: Int?,
        val strokeWeight: Float,
        val opacity: Int,
        val path: Path,
        val referenceBounds: CoordinateBounds,
        val projectedCorners: FloatArray
    )
}

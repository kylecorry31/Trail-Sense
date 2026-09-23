package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import androidx.core.graphics.withMatrix
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.luna.cache.ObjectPool
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPolygon
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geography.Geography
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
import kotlin.math.absoluteValue

class GeoJsonPolygonRenderer : FeatureRenderer() {

    private var filterEpsilon = 0f
    private val clipper = PolygonClipper()
    private var pathPool = ObjectPool { Path() }
    private var polygons = listOf<PrecomputedPolygon>()
    private val lock = Any()
    private var updateListener: (() -> Unit)? = null
    private val transform = GeoJsonRenderTransform()

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
        context: Context,
        viewBounds: Rectangle,
        bounds: CoordinateBounds,
        projection: IMapViewProjection,
        features: List<GeoJsonFeature>
    ) = onDefault {
        val rdp =
            RDPFilter<GeoJsonPosition>(projection.resolutionPixels.coerceAtLeast(1f) * filterEpsilon) { point, start, end ->
                Geography.getCrossTrackDistance(
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
        context: Context,
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
            val projection = map.mapProjection
            for (polygon in polygons) {
                val path = polygon.path

                transform.update(projection, polygon.referenceBounds, polygon.projectedCorners)
                val matrix = transform.matrix
                val relativeScale = transform.scale

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

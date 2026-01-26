package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPolygon
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath.positive
import com.kylecorry.sol.math.SolMath.real
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
                projection.center,
                projection.resolutionPixels
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
                val centerPixel = map.toPixel(polygon.origin)

                drawer.push()
                drawer.translate(centerPixel.x, centerPixel.y)
                val relativeScale = (polygon.renderedScale / map.resolutionPixels).real().positive(1f)
                drawer.scale(relativeScale)

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
                drawer.pop()
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

        // Calculate origin for relative rendering
        val originPixel = projection.toPixels(projection.center)

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
            polygon.path.moveTo(start.x - originPixel.x, start.y - originPixel.y)

            for (i in 1 until clipped.size) {
                val pixel = clipped[i]
                polygon.path.lineTo(pixel.x - originPixel.x, pixel.y - originPixel.y)
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
        val origin: Coordinate,
        val renderedScale: Float
    )
}

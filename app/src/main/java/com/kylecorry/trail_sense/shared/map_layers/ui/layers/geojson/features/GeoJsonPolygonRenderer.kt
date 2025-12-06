package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
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
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import kotlin.math.absoluteValue

class GeoJsonPolygonRenderer : FeatureRenderer() {

    private var reducedPaths = emptyList<PrecomputedPolygon>()
    private var filterEpsilon = 0f
    private val clipper = PolygonClipper()

    private var lastHeight = 0
    private var lastWidth = 0
    private var cachedBounds = Rectangle(0f, 0f, 0f, 0f)

    init {
        setRunInBackgroundWhenChanged(this::renderFeaturesInBackground)
    }

    override fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features.filter { it.geometry is GeoJsonPolygon }
    }

    private suspend fun renderFeaturesInBackground(
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        features: List<GeoJsonFeature>
    ) = onDefault {
        val rdp = RDPFilter<GeoJsonPosition>(metersPerPixel.coerceAtLeast(1f) * filterEpsilon) { point, start, end ->
            Geology.getCrossTrackDistance(
                point.coordinate,
                start.coordinate,
                end.coordinate
            ).value.absoluteValue
        }

        reducedPaths = features.mapNotNull { feature ->
            val geometry = feature.geometry as GeoJsonPolygon
            val rings = geometry.polygon ?: return@mapNotNull null

            val geometryBounds = geometry.boundingBox?.bounds ?: CoordinateBounds.from(rings.flatten().map { it.coordinate })
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
                feature.getOpacity()
            )
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
        val mapBounds = getBounds(drawer)

        drawer.noPathEffect()

        for (polygon in reducedPaths) {
            val path = polygon.path
            path.rewind()
            path.fillType = Path.FillType.EVEN_ODD

            polygon.rings.forEach { ring ->
                if (ring.isEmpty()) return@forEach

                // Convert to pixels
                val pixels = ring.map { map.toPixel(it) }

                // Clip to bounds
                val clipped = clipper.clip(pixels, mapBounds)

                if (clipped.isEmpty()) return@forEach

                val start = clipped[0]
                path.moveTo(start.x, start.y)

                for (i in 1 until clipped.size) {
                    val pixel = clipped[i]
                    path.lineTo(pixel.x, pixel.y)
                }
                path.close()
            }

            if (polygon.fillColor != null) {
                drawer.fill(polygon.fillColor.withAlpha(polygon.opacity))
            } else {
                drawer.noFill()
            }

            if (polygon.strokeColor != null && polygon.strokeWeight > 0) {
                drawer.stroke(polygon.strokeColor.withAlpha(polygon.opacity))
                drawer.strokeWeight(drawer.dp(polygon.strokeWeight * scale))
            } else {
                drawer.noStroke()
            }

            drawer.path(path)
        }

        drawer.noStroke()
        drawer.fill(Color.WHITE)
    }

    private fun getBounds(drawer: ICanvasDrawer): Rectangle {
        if (drawer.canvas.height != lastHeight || drawer.canvas.width != lastWidth) {
            lastHeight = drawer.canvas.height
            lastWidth = drawer.canvas.width
            // Rotating by map rotation wasn't working around 90/270 degrees - this is a workaround
            // It will just render slightly more of the path than needed, but never less (since 45 is when the area is at its largest)
            val bounds = drawer.getBounds(45f)
            val margin = drawer.dp(50f)
            // Top and bottom are flipped to match drawing coordinate system
            cachedBounds = Rectangle(
                bounds.left - margin,
                bounds.bottom - margin,
                bounds.right + margin,
                bounds.top + margin
            )
        }
        return cachedBounds
    }

    class PrecomputedPolygon(
        val feature: GeoJsonFeature,
        val rings: List<List<Coordinate>>,
        val fillColor: Int?,
        val strokeColor: Int?,
        val strokeWeight: Float,
        val opacity: Int,
        val path: Path = Path()
    )
}

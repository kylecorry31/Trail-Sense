package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import android.graphics.Color
import android.util.Log
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getLineStyle
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.extensions.getThicknessScale
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.paths.ui.drawing.PathLineDrawerFactory
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.max

class LineStringLayer : FeatureLayer() {
    private var shouldRenderWithDrawLines = false
    private var shouldRenderSmoothPaths = false
    private var shouldRenderLabels = false

    private val lock = Any()

    private val factory = PathLineDrawerFactory()

    private var currentScale = 1f

    private var backgroundColor: Int? = null

    private var filterEpsilon = 0f

    private var reducedPaths = emptyList<Pair<GeoJsonFeature, FloatArray>>()

    fun setBackgroundColor(color: PathBackgroundColor) {
        backgroundColor = when (color) {
            PathBackgroundColor.None -> null
            PathBackgroundColor.Black -> Color.BLACK
            PathBackgroundColor.White -> Color.WHITE
        }
        invalidate()
    }

    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        this.shouldRenderWithDrawLines = shouldRenderWithDrawLines
        invalidate()
    }

    fun setShouldRenderSmoothPaths(shouldRenderSmoothPaths: Boolean) {
        this.shouldRenderSmoothPaths = shouldRenderSmoothPaths
        invalidate()
    }

    fun setShouldRenderLabels(shouldRenderLabels: Boolean) {
        this.shouldRenderLabels = shouldRenderLabels
        invalidate()
    }

    override fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features.filter { it.geometry is GeoJsonLineString }
    }

    override suspend fun renderFeaturesInBackground(
        bounds: CoordinateBounds,
        metersPerPixel: Float,
        features: List<GeoJsonFeature>
    ) {
        // TODO: Run this less often

        // Apply RDP filter to features
        val rdp =
            RDPFilter<GeoJsonPosition>(metersPerPixel.coerceAtLeast(1f) * filterEpsilon) { point, start, end ->
                Geology.getCrossTrackDistance(
                    point.coordinate,
                    start.coordinate,
                    end.coordinate
                ).value.absoluteValue
            }

        var totalCount = 0
        // TODO: Clip to coordinate bounds
        reducedPaths = features.mapNotNull {
            val geometry = it.geometry as GeoJsonLineString
            val line = geometry.line

            val geometryBounds =
                geometry.boundingBox?.bounds ?: CoordinateBounds.from(line?.map { it.coordinate }
                    ?: emptyList())
            if (!geometryBounds.intersects(bounds)) {
                return@mapNotNull null
            }

            val filteredLine = line?.let { rdp.filter(it) }
            totalCount += filteredLine?.size ?: 0
            it.copy(geometry = geometry.copy(line = filteredLine)) to FloatArray(
                (filteredLine?.size ?: 0) * 4
            )
        }
        Log.d("LineStringLayer", "Rendered $totalCount vertices")
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        if (filterEpsilon == 0f) {
            filterEpsilon = drawer.dp(1.5f)
        }
        val scale = map.layerScale
        currentScale = map.metersPerPixel
        super.draw(drawer, map)
        // Make a copy of the rendered paths
        synchronized(lock) {
            val values = reducedPaths
            for (value in values) {
                val feature = value.first
                val path = feature.geometry as GeoJsonLineString
                val line = path.line ?: emptyList()
                // Don't draw empty paths
                if (line.isEmpty()) {
                    continue
                }
                val pathDrawer = factory.create(feature.getLineStyle() ?: LineStyle.Solid)
                drawer.push()
                drawer.strokeJoin(StrokeJoin.Round)
                drawer.strokeCap(StrokeCap.Round)
                val floatArray = value.second
                // TODO: Do all the drawing except for final render in the background? Maybe SurfaceView will resolve this?
                var lastPixel = map.toPixel(line[0].y, line[0].x)
                for (i in 1..line.lastIndex) {
                    val nextPixel = map.toPixel(line[i].y, line[i].x)
                    floatArray[(i - 1) * 4] = lastPixel.x
                    floatArray[(i - 1) * 4 + 1] = lastPixel.y
                    floatArray[(i - 1) * 4 + 2] = nextPixel.x
                    floatArray[(i - 1) * 4 + 3] = nextPixel.y
                    lastPixel = nextPixel
                }

                backgroundColor?.let { backgroundColor ->
                    factory.create(LineStyle.Solid).draw(
                        drawer,
                        backgroundColor,
                        strokeScale = 0.75f * scale / (feature.getThicknessScale() ?: 1f)
                    ) {
                        lines(floatArray)
                    }
                }
                pathDrawer.draw(
                    drawer,
                    feature.getColor() ?: Color.WHITE,
                    strokeScale = scale / (feature.getThicknessScale() ?: 1f)
                ) {
                    lines(floatArray)
                }

                drawer.pop()
                drawLabels(drawer, map, feature)
            }
        }

        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }


    private fun drawLabels(drawer: ICanvasDrawer, map: IMapView, path: GeoJsonFeature) {
        if (shouldRenderLabels && path.getName() != null) {
            val pts = (path.geometry as GeoJsonLineString).line ?: emptyList()
            if (pts.size < 2) return

            // TODO: Adjust text size / wrapping based on name length
            drawer.textSize(drawer.sp(10f * map.layerScale))
            val strokeWeight = drawer.dp(2.5f * map.layerScale)
            val minSeparationPx = max(drawer.canvas.width, drawer.canvas.height) / 4f
            val maxLabels = 5

            // World-aligned grid for label selection
            val bounds = map.mapBounds
            val zoomLevel = TileMath.distancePerPixelToZoom(
                map.metersPerPixel.toDouble(),
                bounds.center.latitude
            )

            // Only show labels at zoom level 13+
            if (zoomLevel < 13) return

            // Grid spacing based on zoom level (degrees)
            val resolution = when (zoomLevel) {
                13 -> 0.048
                14 -> 0.024
                15 -> 0.016
                16 -> 0.008
                17 -> 0.004
                18 -> 0.002
                else -> 0.001
            }

            // Grid
            val latitudes = Interpolation.getMultiplesBetween(
                bounds.south,
                bounds.north,
                resolution
            )
            val longitudes = Interpolation.getMultiplesBetween(
                bounds.west,
                bounds.east,
                resolution
            )

            val segmentCenters = ArrayList<PixelCoordinate>(pts.size - 1)
            val segmentAngles = ArrayList<Float>(pts.size - 1)
            for (i in 0 until (pts.size - 1)) {
                val p1 = map.toPixel(pts[i].coordinate)
                val p2 = map.toPixel(pts[i + 1].coordinate)
                segmentCenters.add(p1.midpoint(p2))
                segmentAngles.add(p1.angleTo(p2))
            }

            val chosenSegments = HashSet<Int>()
            val placedCenters = mutableListOf<PixelCoordinate>()

            drawer.strokeWeight(strokeWeight)
            drawer.textMode(TextMode.Center)
            drawer.stroke(Color.WHITE)
            drawer.fill(Color.BLACK)
            drawer.noPathEffect()

            var labelsDrawn = 0
            for (lat in latitudes) {
                if (labelsDrawn >= maxLabels) break
                for (lon in longitudes) {
                    if (labelsDrawn >= maxLabels) break
                    val gridPixel = map.toPixel(Coordinate(lat, lon))

                    // Find nearest segment center to this grid point
                    val closestIndex =
                        SolMath.argmin(segmentCenters.map { it.distanceTo(gridPixel) })

                    if (closestIndex >= 0) {
                        val center = segmentCenters[closestIndex]
                        if (chosenSegments.contains(closestIndex)) continue
                        if (placedCenters.any { it.distanceTo(center) < minSeparationPx }) continue

                        chosenSegments.add(closestIndex)
                        placedCenters.add(center)
                        val angle = segmentAngles[closestIndex]
                        val drawAngle = if (angle.absoluteValue > 90) angle + 180 else angle

                        drawer.push()
                        drawer.rotate(drawAngle, center.x, center.y)
                        drawer.text(path.getName() ?: "", center.x, center.y)
                        drawer.pop()
                        labelsDrawn++
                    }
                }
            }
        }
    }

    private fun PixelCoordinate.midpoint(other: PixelCoordinate): PixelCoordinate {
        return PixelCoordinate(
            (this.x + other.x) / 2,
            (this.y + other.y) / 2
        )
    }

    private fun PixelCoordinate.angleTo(other: PixelCoordinate): Float {
        return atan2(
            other.y - this.y,
            other.x - this.x
        ).toDegrees()
    }

}
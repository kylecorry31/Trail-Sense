package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.positive
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.extensions.drawLines
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getLineStyle
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.extensions.getThicknessScale
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.paths.ui.drawing.PathLineDrawerFactory
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.max

class GeoJsonLineStringRenderer : FeatureRenderer() {
    private var shouldRenderWithDrawLines = false
    private var shouldRenderSmoothPaths = false
    private var shouldRenderLabels = false
    private val factory = PathLineDrawerFactory()
    private val clipper = LineClipper()
    private var pathPool = ObjectPool { Path() }
    private var backgroundColor: Int? = null
    private var filterEpsilon = 0f
    private var reducedPaths = emptyList<PrecomputedLineString>()
    private var labelGrid = PrecomputedLabelGrid(emptyList(), emptyList())
    private val lock = Any()

    init {
        setRunInBackgroundWhenChanged(this::renderFeaturesInBackground)
    }

    private fun render(
        points: List<Coordinate>,
        path: Path,
        bounds: Rectangle,
        projection: IMapViewProjection
    ): FloatArray {
        val originPixel = projection.toPixels(projection.center)
        val pixels = points.map { projection.toPixels(it) }
        val segments = mutableListOf<Float>()

        clipper.clip(pixels, bounds, segments, originPixel)

        val segmentArray = segments.toFloatArray()
        path.reset()
        if (segmentArray.isEmpty()) {
            return segmentArray
        }

        if (!shouldRenderWithDrawLines) {
            path.drawLines(segmentArray, shouldRenderSmoothPaths)
        }

        return segmentArray
    }

    fun setBackgroundColor(color: PathBackgroundColor) {
        backgroundColor = when (color) {
            PathBackgroundColor.None -> null
            PathBackgroundColor.Black -> Color.BLACK
            PathBackgroundColor.White -> Color.WHITE
        }
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
    }

    override fun filterFeatures(features: List<GeoJsonFeature>): List<GeoJsonFeature> {
        return features.filter { it.geometry is GeoJsonLineString }
    }

    private suspend fun renderFeaturesInBackground(
        viewBounds: Rectangle,
        bounds: CoordinateBounds,
        projection: IMapViewProjection,
        features: List<GeoJsonFeature>
    ) = onDefault {
        // Canvas bounds are inverted
        val margin = 100f
        val actualViewBounds = Rectangle(
            viewBounds.left - margin,
            viewBounds.top + margin,
            viewBounds.right + margin,
            viewBounds.bottom - margin
        )

        val precomputed = features.mapNotNull {
            val geometry = it.geometry as GeoJsonLineString
            val line = geometry.line

            val geometryBounds =
                geometry.boundingBox?.bounds
                    ?: CoordinateBounds.from(line?.map { pos -> pos.coordinate }
                        ?: emptyList())
            if (!geometryBounds.intersects(bounds)) {
                return@mapNotNull null
            }

            val coordinates = line?.map { pos -> pos.coordinate } ?: emptyList()
            if (coordinates.isEmpty()) {
                return@mapNotNull null
            }

            val path = pathPool.get()
            val lineSegments = render(coordinates, path, actualViewBounds, projection)
            if (lineSegments.isEmpty()) {
                pathPool.release(path)
                return@mapNotNull null
            }

            PrecomputedLineString(
                it,
                coordinates,
                lineSegments,
                it.getName(),
                it.getColor() ?: Color.WHITE,
                it.getLineStyle() ?: LineStyle.Solid,
                it.getThicknessScale() ?: 1f,
                path,
                projection.center,
                projection.metersPerPixel
            )
        }

        val zoomLevel = TileMath.distancePerPixelToZoom(
            projection.metersPerPixel.toDouble(),
            bounds.center.latitude
        )

        // Only show labels at zoom level 13+
        if (zoomLevel >= 13) {
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

            labelGrid = PrecomputedLabelGrid(latitudes, longitudes)
        } else {
            labelGrid = PrecomputedLabelGrid(emptyList(), emptyList())
        }

        synchronized(lock) {
            reducedPaths.forEach { pathPool.release(it.path) }
            reducedPaths = precomputed
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
            for (path in reducedPaths) {
                if (path.line.isEmpty()) {
                    continue
                }

                val pathDrawer = factory.create(path.lineStyle)
                val centerPixel = map.toPixel(path.origin)

                drawer.push()
                drawer.translate(centerPixel.x, centerPixel.y)
                val relativeScale = (path.renderedScale / map.metersPerPixel).real().positive(1f)
                drawer.scale(relativeScale)
                drawer.strokeJoin(StrokeJoin.Round)
                drawer.strokeCap(StrokeCap.Round)

                backgroundColor?.let { backgroundColor ->
                    factory.create(LineStyle.Solid).draw(
                        drawer,
                        backgroundColor,
                        strokeScale = 0.75f * scale / (path.thicknessScale * relativeScale)
                    ) {
                        if (shouldRenderWithDrawLines) {
                            lines(path.line)
                        } else {
                            path(path.path)
                        }
                    }
                }

                pathDrawer.draw(
                    drawer,
                    path.color,
                    strokeScale = scale / (path.thicknessScale * relativeScale)
                ) {
                    if (shouldRenderWithDrawLines) {
                        lines(path.line)
                    } else {
                        path(path.path)
                    }
                }

                drawer.pop()
                drawLabels(drawer, map, path)
            }
        }

        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }


    private fun drawLabels(drawer: ICanvasDrawer, map: IMapView, path: PrecomputedLineString) {
        if (shouldRenderLabels && path.name != null) {
            if (path.points.size < 2) return

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

            // TODO: Precompute all of this
            val segmentCenters = ArrayList<PixelCoordinate>(path.points.size - 1)
            val segmentAngles = ArrayList<Float>(path.points.size - 1)
            for (i in 0 until (path.points.size - 1)) {
                val p1 = map.toPixel(path.points[i])
                val p2 = map.toPixel(path.points[i + 1])
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

            val grid = labelGrid

            var labelsDrawn = 0
            for (lat in grid.latitudes) {
                if (labelsDrawn >= maxLabels) break
                for (lon in grid.longitudes) {
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
                        drawer.text(path.name, center.x, center.y)
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

    class PrecomputedLineString(
        val feature: GeoJsonFeature,
        val points: List<Coordinate>,
        val line: FloatArray,
        val name: String?,
        val color: Int,
        val lineStyle: LineStyle,
        val thicknessScale: Float,
        val path: Path,
        val origin: Coordinate,
        val renderedScale: Float
    )

    class PrecomputedLabelGrid(
        val latitudes: List<Double>,
        val longitudes: List<Double>,
    )

}
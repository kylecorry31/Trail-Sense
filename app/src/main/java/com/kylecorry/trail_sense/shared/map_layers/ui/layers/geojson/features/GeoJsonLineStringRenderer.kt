package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.withMatrix
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.math.lists.Lists
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.extensions.DEFAULT_LINE_STRING_STROKE_WEIGHT_DP
import com.kylecorry.trail_sense.shared.extensions.drawLines
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getLineStyle
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.extensions.getStrokeWeight
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.paths.ui.drawing.PathLineDrawerFactory
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

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
    private val lock = Any()
    private val matrix = Matrix()
    private val src = FloatArray(8)
    private val dst = FloatArray(8)
    private val labelPoint = FloatArray(2)

    init {
        setRunInBackgroundWhenChanged(this::renderFeaturesInBackground)
    }

    private fun render(
        pixels: List<PixelCoordinate>,
        path: Path,
        bounds: Rectangle
    ): FloatArray {
        val segments = mutableListOf<Float>()

        clipper.clip(pixels, bounds, segments, rdpFilterEpsilon = filterEpsilon)

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
        context: Context,
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

            val pixelPoints = coordinates.map { coordinate -> projection.toPixels(coordinate) }
            val path = pathPool.get()
            val lineSegments = render(pixelPoints, path, actualViewBounds)
            if (lineSegments.isEmpty()) {
                pathPool.release(path)
                return@mapNotNull null
            }

            val name = it.getName()
            val labelSegments = if (shouldRenderLabels && name != null) {
                pixelPoints.windowed(2).map { segment ->
                    val p1 = segment[0]
                    val p2 = segment[1]
                    LabelSegment(p1.midpoint(p2), p1.angleTo(p2))
                }
            } else {
                emptyList()
            }

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

            PrecomputedLineString(
                it,
                coordinates,
                lineSegments,
                name,
                it.getColor() ?: Color.TRANSPARENT,
                it.getLineStyle() ?: LineStyle.Solid,
                (it.getStrokeWeight()
                    ?: DEFAULT_LINE_STRING_STROKE_WEIGHT_DP) / DEFAULT_LINE_STRING_STROKE_WEIGHT_DP,
                labelSegments,
                path,
                bounds,
                projectedCorners
            )
        }

        val zoomLevel = projection.zoom.roundToInt()

        val gridPoints = if (shouldRenderLabels && zoomLevel >= 13) {
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

            val points = mutableListOf<PixelCoordinate>()
            for (lat in latitudes) {
                for (lon in longitudes) {
                    points.add(projection.toPixels(Coordinate(lat, lon)))
                }
            }
            points
        } else {
            emptyList()
        }

        if (shouldRenderLabels && gridPoints.isNotEmpty()) {
            val canvasWidth = (viewBounds.right - viewBounds.left).absoluteValue
            val canvasHeight = (viewBounds.bottom - viewBounds.top).absoluteValue
            val minSeparationPx = max(canvasWidth, canvasHeight) / 4f
            val maxLabels = 5
            computeLabelPlacements(precomputed, gridPoints, minSeparationPx, maxLabels)
        }

        synchronized(lock) {
            reducedPaths.forEach { pathPool.release(it.path) }
            reducedPaths = precomputed
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
        // Paths were originally 6px, so convert that to the default dp size
        val dpScale = 6f / drawer.dp(DEFAULT_LINE_STRING_STROKE_WEIGHT_DP)
        synchronized(lock) {
            for (path in reducedPaths) {
                if (path.line.isEmpty()) {
                    continue
                }

                val currentNW = map.toPixel(path.referenceBounds.northWest)
                val currentNE = map.toPixel(path.referenceBounds.northEast)
                val currentSE = map.toPixel(path.referenceBounds.southEast)
                val currentSW = map.toPixel(path.referenceBounds.southWest)

                // Source points (precomputed relative to origin)
                // NW
                src[0] = path.projectedCorners[0]
                src[1] = path.projectedCorners[1]
                // NE
                src[2] = path.projectedCorners[2]
                src[3] = path.projectedCorners[3]
                // SE
                src[4] = path.projectedCorners[4]
                src[5] = path.projectedCorners[5]
                // SW
                src[6] = path.projectedCorners[6]
                src[7] = path.projectedCorners[7]

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

                val pathDrawer = factory.create(path.lineStyle)

                drawer.canvas.withMatrix(matrix) {
                    drawer.strokeJoin(StrokeJoin.Round)
                    drawer.strokeCap(StrokeCap.Round)

                    backgroundColor?.let { backgroundColor ->
                        factory.create(LineStyle.Solid).draw(
                            drawer,
                            backgroundColor,
                            strokeScale = dpScale * 0.75f * relativeScale * scale / path.thicknessScale
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
                        strokeScale = dpScale * relativeScale * scale / path.thicknessScale
                    ) {
                        if (shouldRenderWithDrawLines) {
                            lines(path.line)
                        } else {
                            path(path.path)
                        }
                    }
                }
                drawLabels(drawer, map, path, matrix)
            }
        }

        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }


    private fun drawLabels(
        drawer: ICanvasDrawer,
        map: IMapView,
        path: PrecomputedLineString,
        matrix: Matrix
    ) {
        if (shouldRenderLabels && path.name != null) {
            if (path.points.size < 2) return

            // TODO: Adjust text size / wrapping based on name length
            drawer.textSize(drawer.sp(10f * map.layerScale))
            val strokeWeight = drawer.dp(2.5f * map.layerScale)
            val maxLabels = 5

            drawer.strokeWeight(strokeWeight)
            drawer.textMode(TextMode.Center)
            drawer.stroke(Color.WHITE)
            drawer.fill(Color.BLACK)
            drawer.noPathEffect()

            val placements = path.labelPlacements
            if (placements.isEmpty()) return

            // Calculate rotation from matrix
            // We can map a vector (1,0) to get angle
            labelPoint[0] = 0f
            labelPoint[1] = 0f
            matrix.mapPoints(labelPoint)
            val p0x = labelPoint[0]
            val p0y = labelPoint[1]

            labelPoint[0] = 1f
            labelPoint[1] = 0f
            matrix.mapPoints(labelPoint)
            val p1x = labelPoint[0]
            val p1y = labelPoint[1]

            val rotationDelta = atan2(p1y - p0y, p1x - p0x).toDegrees()


            var labelsDrawn = 0
            for (placement in placements) {
                if (labelsDrawn >= maxLabels) break

                labelPoint[0] = placement.center.x
                labelPoint[1] = placement.center.y
                matrix.mapPoints(labelPoint)

                val center = PixelCoordinate(labelPoint[0], labelPoint[1])
                val drawAngle = placement.angle + rotationDelta

                drawer.push()
                drawer.rotate(drawAngle, center.x, center.y)
                drawer.text(path.name, center.x, center.y)
                drawer.pop()
                labelsDrawn++
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
        var labelSegments: List<LabelSegment>,
        val path: Path,
        val referenceBounds: CoordinateBounds,
        val projectedCorners: FloatArray,
        var labelPlacements: List<LabelPlacement> = emptyList()
    )

    class LabelSegment(
        val center: PixelCoordinate,
        val angle: Float
    )

    class LabelPlacement(
        val center: PixelCoordinate,
        val angle: Float
    )

    private fun computeLabelPlacements(
        paths: List<PrecomputedLineString>,
        gridPoints: List<PixelCoordinate>,
        minSeparationPx: Float,
        maxLabels: Int
    ) {
        for (path in paths) {
            val segments = path.labelSegments
            if (segments.isEmpty()) {
                path.labelPlacements = emptyList()
                continue
            }

            val placements = mutableListOf<LabelPlacement>()
            val chosenSegments = HashSet<Int>()
            val placedCenters = mutableListOf<PixelCoordinate>()

            for (gridPixel in gridPoints) {
                if (placements.size >= maxLabels) break

                val closestIndex =
                    Lists.argmin(segments.map { it.center.distanceTo(gridPixel) })

                if (closestIndex >= 0) {
                    val center = segments[closestIndex].center
                    if (chosenSegments.contains(closestIndex)) continue
                    if (placedCenters.any { it.distanceTo(center) < minSeparationPx }) continue

                    chosenSegments.add(closestIndex)
                    placedCenters.add(center)

                    val angle = segments[closestIndex].angle
                    val drawAngle = if (angle.absoluteValue > 90) angle + 180 else angle

                    placements.add(LabelPlacement(center, drawAngle))
                }
            }

            path.labelPlacements = placements
            path.labelSegments = emptyList()
        }
    }

}

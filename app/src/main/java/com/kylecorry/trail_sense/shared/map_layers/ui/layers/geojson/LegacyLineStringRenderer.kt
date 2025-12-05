package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.positive
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.drawLines
import com.kylecorry.trail_sense.shared.extensions.getColor
import com.kylecorry.trail_sense.shared.extensions.getLineStyle
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.extensions.getThicknessScale
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.paths.ui.drawing.ClippedPathRenderer
import com.kylecorry.trail_sense.tools.paths.ui.drawing.IRenderedPathFactory
import com.kylecorry.trail_sense.tools.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.tools.paths.ui.drawing.RenderedPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.max

class LegacyLineStringRenderer : FeatureRenderer(), ILineStringRenderer {

    private var pathsRendered = false
    private var renderInProgress = false
    private var linePool = ObjectPool { mutableListOf<Float>() }
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = listOf<RenderedPath>()
    private var updateListener: (() -> Unit)? = null

    private var shouldRenderWithDrawLines = false
    private var shouldRenderSmoothPaths = false
    private var shouldRenderLabels = false

    private val lock = Any()

    private val runner = CoroutineQueueRunner()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val factory = PathLineDrawerFactory()

    private var currentScale = 1f

    private var backgroundColor: Int? = null

    private var lastHeight = 0
    private var lastWidth = 0
    private var cachedBounds = Rectangle(0f, 0f, 0f, 0f)

    private var filterEpsilon = 0f

    override fun setBackgroundColor(color: PathBackgroundColor) {
        backgroundColor = when (color) {
            PathBackgroundColor.None -> null
            PathBackgroundColor.Black -> Color.BLACK
            PathBackgroundColor.White -> Color.WHITE
        }
    }

    override fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        this.shouldRenderWithDrawLines = shouldRenderWithDrawLines
    }

    override fun setShouldRenderSmoothPaths(shouldRenderSmoothPaths: Boolean) {
        this.shouldRenderSmoothPaths = shouldRenderSmoothPaths
    }

    override fun setShouldRenderLabels(shouldRenderLabels: Boolean) {
        this.shouldRenderLabels = shouldRenderLabels
    }

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView,
        features: List<GeoJsonFeature>
    ) {
        if (filterEpsilon == 0f) {
            filterEpsilon = drawer.dp(1.5f)
        }

        val scale = map.layerScale
        currentScale = map.metersPerPixel
        if (!pathsRendered && !renderInProgress) {
            val renderer = ClippedPathRenderer(
                getBounds(drawer),
                map::toPixel,
                filterEpsilon
            )
            renderInBackground(renderer, features)
        }

        // Make a copy of the rendered paths
        synchronized(lock) {
            for (path in renderedPaths) {
                // Don't draw empty paths
                if (path.line.isEmpty()) {
                    continue
                }
                val pathDrawer = factory.create(path.style)
                val centerPixel = map.toPixel(path.origin)
                drawer.push()
                drawer.translate(centerPixel.x, centerPixel.y)
                val relativeScale = (path.renderedScale / currentScale).real().positive(1f)
                drawer.scale(relativeScale)
                drawer.strokeJoin(StrokeJoin.Round)
                drawer.strokeCap(StrokeCap.Round)
                backgroundColor?.let { backgroundColor ->
                    factory.create(LineStyle.Solid).draw(
                        drawer,
                        backgroundColor,
                        strokeScale = 0.75f * scale / (path.originalFeature?.getThicknessScale()
                            ?: 1f)
                    ) {
                        if (shouldRenderWithDrawLines || path.path == null) {
                            lines(path.line)
                        } else {
                            path(path.path)
                        }
                    }
                }
                pathDrawer.draw(
                    drawer,
                    path.color,
                    strokeScale = scale / (path.originalFeature?.getThicknessScale() ?: 1f)
                ) {
                    if (shouldRenderWithDrawLines || path.path == null) {
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

    private fun drawLabels(drawer: ICanvasDrawer, map: IMapView, path: RenderedPath) {
        if (shouldRenderLabels && path.originalFeature?.getName() != null) {
            val pts = (path.originalFeature.geometry as GeoJsonLineString).line ?: emptyList()
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
                        drawer.text(path.originalFeature.getName() ?: "", center.x, center.y)
                        drawer.pop()
                        labelsDrawn++
                    }
                }
            }
        }
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    private fun renderInBackground(renderer: IRenderedPathFactory, features: List<GeoJsonFeature>) {
        renderInProgress = true
        scope.launch {
            runner.replace {
                render(renderer, features)
                onMain {
                    updateListener?.invoke()
                }
            }
        }
    }

    private suspend fun render(renderer: IRenderedPathFactory, features: List<GeoJsonFeature>) {
        // Render the paths
        val updated = render(features, renderer)

        // Update the rendered paths
        synchronized(lock) {
            renderedPaths.forEach {
                it.path?.let {
                    pathPool.release(it)
                }
            }
            renderedPaths = updated
        }

        pathsRendered = true
        renderInProgress = false
    }

    private suspend fun render(
        features: List<GeoJsonFeature>,
        renderer: IRenderedPathFactory
    ): List<RenderedPath> {
        val map = mutableListOf<RenderedPath>()
        for (feature in features) {
            val lineObj = linePool.get()
            val pathObj = if (shouldRenderWithDrawLines) null else pathPool.get()
            map.add(render(feature, renderer, lineObj, pathObj))
            linePool.release(lineObj)
        }
        return map
    }

    private suspend fun render(
        feature: GeoJsonFeature,
        renderer: IRenderedPathFactory,
        lineObj: MutableList<Float>,
        pathObj: Path?
    ): RenderedPath = onDefault {
        val points =
            (feature.geometry as GeoJsonLineString).line?.map { it.coordinate } ?: emptyList()
        lineObj.clear()
        pathObj?.reset()
        val before = currentScale
        val rendered = renderer.render(points, lineObj)
        rendered.copy(
            style = feature.getLineStyle() ?: LineStyle.Solid,
            color = feature.getColor() ?: Color.WHITE,
            // A best guess at what scale the path was rendered at
            renderedScale = (before + currentScale) / 2f,
            originalFeature = feature,
            path = pathObj?.also {
                it.drawLines(rendered.line, shouldRenderSmoothPaths)
            })
    }

    override fun invalidate() {
        pathsRendered = false
        renderInProgress = false
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun getBounds(drawer: ICanvasDrawer): Rectangle {
        if (drawer.canvas.height != lastHeight || drawer.canvas.width != lastWidth) {
            lastHeight = drawer.canvas.height
            lastWidth = drawer.canvas.width
            // Rotating by map rotation wasn't working around 90/270 degrees - this is a workaround
            // It will just render slightly more of the path than needed, but never less (since 45 is when the area is at its largest)
            cachedBounds = drawer.getBounds(45f)
        }
        return cachedBounds
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
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

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity
}
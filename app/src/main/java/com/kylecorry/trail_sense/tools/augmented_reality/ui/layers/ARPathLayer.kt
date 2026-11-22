package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import kotlin.math.roundToInt
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.arithmetic.Arithmetic.square
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.canvas.LineInterpolator
import com.kylecorry.trail_sense.shared.extensions.isSamePixel
import com.kylecorry.trail_sense.shared.forEachLine
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.GeographicARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.shared.map_layers.MappablePath
import com.kylecorry.trail_sense.shared.map_layers.MappableLocation
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.shared.map_layers.IMappablePath

/**
 * An augmented reality layer that displays paths
 * @param viewDistanceMeters the maximum distance to display paths
 * @param adjustForPathElevation true if the layer should adjust for path elevation, otherwise paths will be 2 meters below the camera
 * @param onFocus the callback to call when a path is focused, return true if focus is claimed
 */
class ARPathLayer(
    viewDistanceMeters: Float,
    private val adjustForPathElevation: Boolean,
    private val onFocus: (path: IMappablePath) -> Boolean = { false },
) : ARLayer {

    private val lineLayer = ARLineLayer(renderWithPaths = false)
    private val markerLayer = ARMarkerLayer(1f, 32f, false)
    private val polygons = ARPathPolygons(widthMeters = 0.45f)
    private val ribbonLayer = ARPolygonLayer()
    private val arrowLayer = ARPolygonLayer()
    private val wallArrowLayer = ARPolygonLayer()
    private var lastElevation: Float? = null
    private var lastLocationAccuracySquared: Float? = null

    private val squareViewDistance = square(viewDistanceMeters)
    private val center = PixelCoordinate(viewDistanceMeters, viewDistanceMeters)
    private val bounds = Rectangle(
        0f,
        viewDistanceMeters * 2,
        viewDistanceMeters * 2,
        0f,
    )
    private val clipper = LineClipper()
    private val interpolator = LineInterpolator()

    private val pointSpacing = 2f // meters
    private val pathSimplification = 0.5f // meters
    private val snapDistance = 2 * viewDistanceMeters / 3f // meters
    private val snapDistanceSquared = square(snapDistance)

    private val maxElevationOffset = maxOf(5f, viewDistanceMeters / 4f) // meters
    private val defaultElevationOffset = -2f // meters

    private var projection: IMapProjection? = null
    private var groundOffset = defaultElevationOffset

    private var paths: List<IMappablePath> = listOf()

    var destination: Destination.Path? = null

    @Volatile
    var appearance: ARPathAppearance = ARPathAppearance.Ribbon

    private val ribbonLayers = listOf<ARLayer>(ribbonLayer, arrowLayer)
    private val wallLayers = listOf<ARLayer>(ribbonLayer, wallArrowLayer)
    private val lineLayers = listOf<ARLayer>(lineLayer, markerLayer)
    private val activeLayers: List<ARLayer>
        get() = when (appearance) {
            ARPathAppearance.Ribbon -> ribbonLayers
            ARPathAppearance.Line -> lineLayers
            ARPathAppearance.Walls -> wallLayers
        }

    private val hooks = Hooks()

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        lastElevation = view.altitude
        groundOffset = view.groundOffset ?: defaultElevationOffset
        lastLocationAccuracySquared = view.locationAccuracy?.let { square(it) }

        hooks.effect(
            "projection",
            view.location
        ) {
            projection = AzimuthalEquidistantProjection(
                view.location,
                Vector2(center.x, center.y),
                isYFlipped = true
            )
        }

        hooks.effect(
            "paths",
            view.location,
            if (adjustForPathElevation) view.altitude else null,
            view.locationAccuracy,
            paths,
            destination,
            destination?.route?.guidance,
            (groundOffset * 10).roundToInt()
        ) {
            updatePaths(getPaths())
        }

        activeLayers.forEach { it.update(drawer, view) }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        activeLayers.forEach { it.draw(drawer, view) }
    }

    override fun invalidate() {
        ribbonLayer.invalidate()
        arrowLayer.invalidate()
        wallArrowLayer.invalidate()
        lineLayer.invalidate()
        markerLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return activeLayers.any { it.onFocus(drawer, view) }
    }

    fun setPaths(paths: List<IMappablePath>) {
        this.paths = paths
    }

    internal fun getPaths(): List<IMappablePath> {
        val destination = destination ?: return paths
        val path = destination.path
        val guidance = destination.route.guidance
        val route = MappablePath(
            path.id,
            guidance.remainingRoute.mapIndexed { index, point ->
                MappableLocation(
                    index.toLong(), point.coordinate, path.style.color, null,
                    elevation = point.elevation
                )
            },
            path.style.color,
            LineStyle.Arrow,
            path.name
        )
        return paths.filterNot { it.id == path.id } + route
    }

    private fun updatePaths(paths: List<IMappablePath>) {
        val elevation = lastElevation

        val points = paths.map {
            it to getNearbyARPoints(it, elevation)
        }

        val nearestPoints = points.mapNotNull { getNearestPoint(it.second) }
        val nearest = nearestPoints.minByOrNull { it.point.squaredDistanceTo(center) }
        if (nearest != null) {
            if (nearest.previousIndex != null && nearest.nextIndex != null) {
                nearest.addToPath()
            }


            points.forEach {
                recenterPoints(it.second.first, nearest.point, center)
            }

            if (adjustForPathElevation) {
                val elevationDelta = elevation?.minus(nearest.elevation) ?: 0f
                points.forEach {
                    it.second.second.forEachIndexed { index, _ ->
                        it.second.second[index] = it.second.second[index] + elevationDelta
                    }
                }
            }
        }


        val lines = points.flatMap { (path, pts) ->
            project(pts).map {
                path to ARLine(it, path.color, 4f)
            }
        }
        val markers = lines.flatMap { (path, line) ->
            line.points.map { point ->
                ARMarker(
                    point,
                    CanvasCircle(line.color),
                    onFocusedFn = { onFocus(path) }
                )
            }
        }

        markerLayer.setMarkers(markers)
        lineLayer.setLines(lines.map { it.second })
        val navigationPathId = destination?.path?.id
        ribbonLayer.setSource { view ->
            lines.map { (path, line) -> polygons.ribbon(line, view) { onFocus(path) } }
        }
        wallArrowLayer.setSource { view ->
            lines.filter { (path, _) -> path.id == navigationPathId }
                .flatMap { (_, line) -> polygons.wallArrows(line, view) }
        }
        arrowLayer.setSource { view ->
            lines.filter { (path, _) -> path.id == navigationPathId }
                .map { (_, line) -> polygons.arrows(line, view) }
        }
    }

    private fun getNearestPoint(points: Pair<MutableList<Float>, MutableList<Float>>): NearestPoint? {
        var minPoint: PixelCoordinate? = null
        var minPointIndex: Int? = null
        var minDistance =
            lastLocationAccuracySquared?.coerceAtLeast(snapDistanceSquared) ?: snapDistanceSquared
        var minPointElevation: Float? = null

        var i = 0
        points.first.forEachLine { x1, y1, x2, y2 ->
            val z1 = points.second.getOrNull(i) ?: lastElevation ?: 0f
            val z2 = points.second.getOrNull(i + 1) ?: lastElevation ?: 0f
            val projected = projectOntoLine(center.x, center.y, x1, y1, z1, x2, y2, z2)
            val distance = projected.first.squaredDistanceTo(center)

            if (distance < minDistance) {
                minDistance = distance
                minPoint = projected.first
                minPointElevation = projected.second
                minPointIndex = i
            }

            i += 2
        }

        return NearestPoint(
            minPoint ?: return null,
            minPointElevation ?: return null,
            points,
            minPointIndex,
            minPointIndex?.let { it + 1 }
        )
    }

    private fun project(
        points: Pair<MutableList<Float>, MutableList<Float>>
    ): List<List<ARPoint>> {
        val lines = mutableListOf<List<ARPoint>>()
        var currentLine = mutableListOf<ARPoint>()

        var lastPixel: PixelCoordinate? = null
        var i = 0

        points.first.forEachLine { x1, y1, x2, y2 ->
            val pixel1 = PixelCoordinate(x1, y1)
            val pixel2 = PixelCoordinate(x2, y2)
            val last = lastPixel
            val elevation1 = points.second.getOrNull(i) ?: lastElevation
            val elevation2 = points.second.getOrNull(i + 1) ?: lastElevation
            i += 2

            if (last != null && !last.isSamePixel(pixel1) || currentLine.isEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                val spherical = toARPoint(pixel1, elevation1)

                if (spherical == null) {
                    lastPixel = null
                    return@forEachLine
                }

                currentLine.add(spherical)
            }

            val spherical = toARPoint(pixel2, elevation2)
            if (spherical == null) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                lastPixel = null
                return@forEachLine
            }

            currentLine.add(spherical)
            lastPixel = pixel2
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }


        return lines.filterNot { it.size < 2 }
    }

    private fun getNearbyARPoints(
        path: IMappablePath,
        elevation: Float?
    ): Pair<MutableList<Float>, MutableList<Float>> {
        val projected = path.points.map {
            project(it.coordinate)
        }

        val z = path.points.map { it.elevation ?: elevation ?: 0f }


        val output = mutableListOf<Float>()
        val zOutput = mutableListOf<Float>()
        clipper.clip(
            projected,
            bounds,
            output,
            rdpFilterEpsilon = pathSimplification,
            zValues = z,
            zOutput = zOutput
        )

        val output2 = mutableListOf<Float>()
        val zOutput2 = mutableListOf<Float>()
        interpolator.increaseResolution(
            output,
            output2,
            pointSpacing,
            zOutput,
            zOutput2
        )

        return output2 to zOutput2
    }

    private fun recenterPoints(
        points: MutableList<Float>,
        oldCenter: PixelCoordinate,
        newCenter: PixelCoordinate
    ) {
        for (i in points.indices step 2) {
            val x = points[i]
            val y = points[i + 1]
            points[i] = x - oldCenter.x + newCenter.x
            points[i + 1] = y - oldCenter.y + newCenter.y
        }
    }

    private fun projectOntoLine(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float
    ): Pair<PixelCoordinate, Float> {
        val snapped = Geometry.snapToLine(
            x, y,
            x1, y1, z1,
            x2, y2, z2
        )
        return PixelCoordinate(snapped.x, snapped.y) to snapped.z
    }

    private fun project(
        location: Coordinate
    ): PixelCoordinate {
        return projection?.toPixels(location)?.let {
            PixelCoordinate(it.x, it.y)
        } ?: PixelCoordinate(0f, 0f)
    }

    private fun toLocation(pixel: PixelCoordinate): Coordinate? {
        val squareDistance = pixel.squaredDistanceTo(center)
        if (squareDistance > squareViewDistance) {
            return null
        }

        return projection?.toCoordinate(Vector2(pixel.x, pixel.y))
    }

    private fun toARPoint(pixel: PixelCoordinate, elevation: Float?): ARPoint? {
        val location = toLocation(pixel) ?: return null
        val elevationOffset = if (adjustForPathElevation && elevation != null) {
            (elevation - (lastElevation ?: 0f) + groundOffset)
        } else {
            groundOffset
        }
        return GeographicARPoint(
            location,
            elevationOffset.coerceIn(-maxElevationOffset, maxElevationOffset),
            isElevationRelative = true,
            actualDiameter = 0.25f
        )
    }

    private class NearestPoint(
        val point: PixelCoordinate,
        val elevation: Float,
        val path: Pair<MutableList<Float>, MutableList<Float>>,
        val previousIndex: Int?,
        val nextIndex: Int?
    ) {
        fun addToPath() {
            if (previousIndex == null || nextIndex == null) {
                return
            }

            path.first.add(previousIndex * 2 + 2, point.x)
            path.first.add(previousIndex * 2 + 3, point.y)
            path.first.add(previousIndex * 2 + 4, point.x)
            path.first.add(previousIndex * 2 + 5, point.y)
            path.second.add(previousIndex + 1, elevation)
            path.second.add(previousIndex + 2, elevation)
        }
    }
}

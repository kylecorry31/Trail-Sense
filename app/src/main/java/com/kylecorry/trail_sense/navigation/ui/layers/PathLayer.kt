package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.math.SolMath.positive
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.ClippedPathRenderer
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.IRenderedPathFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPath
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.shared.extensions.drawLines
import com.kylecorry.trail_sense.shared.getBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PathLayer : ILayer {

    private var pathsRendered = false
    private var renderInProgress = false
    private var linePool = ObjectPool { mutableListOf<Float>() }
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private val _paths =
        mutableListOf<IMappablePath>() // TODO: Make this Pair<Path, List<PathPoint>>

    private var shouldRenderWithDrawLines = false

    private val lock = Any()

    private val runner = CoroutineQueueRunner()
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentScale = 1f

    fun setShouldRenderWithDrawLines(shouldRenderWithDrawLines: Boolean) {
        this.shouldRenderWithDrawLines = shouldRenderWithDrawLines
        invalidate()
    }

    fun setPaths(paths: List<IMappablePath>) {
        synchronized(lock) {
            _paths.clear()
            _paths.addAll(paths)
            invalidate()
        }
    }

    override fun draw(drawer: ICanvasDrawer, map: IMapView) {
        val scale = map.layerScale
        currentScale = map.metersPerPixel
        if (!pathsRendered && !renderInProgress) {
            val renderer = ClippedPathRenderer(
                getBounds(drawer),
                map::toPixel,
                drawer.dp(1f)
            )
            renderInBackground(renderer)
        }

        // Make a copy of the rendered paths
        synchronized(lock) {
            val factory = PathLineDrawerFactory()
            val values = renderedPaths.values
            for (path in values) {
                val pathDrawer = factory.create(path.style)
                val centerPixel = map.toPixel(path.origin)
                drawer.push()
                drawer.translate(centerPixel.x, centerPixel.y)
                val relativeScale = (path.renderedScale / currentScale).real().positive(1f)
                drawer.scale(relativeScale)
                drawer.strokeJoin(StrokeJoin.Round)
                drawer.strokeCap(StrokeCap.Round)
                pathDrawer.draw(drawer, path.color, strokeScale = scale) {
                    if (shouldRenderWithDrawLines || path.path == null) {
                        lines(path.line.toFloatArray())
                    } else {
                        path(path.path)
                    }
                }
                drawer.pop()
            }
        }
        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }

    private fun renderInBackground(renderer: IRenderedPathFactory) {
        renderInProgress = true
        scope.launch {
            runner.replace {
                render(renderer)
            }
        }
    }

    private suspend fun render(renderer: IRenderedPathFactory) {
        // Make a copy of the paths to render
        val paths = synchronized(lock) {
            _paths.toList()
        }

        // Render the paths
        val updated = render(paths, renderer)

        // Update the rendered paths
        synchronized(lock) {
            renderedPaths.forEach {
                linePool.release(it.value.line)
                it.value.path?.let {
                    pathPool.release(it)
                }
            }
            renderedPaths = updated
        }

        pathsRendered = true
        renderInProgress = false
    }

    private suspend fun render(
        paths: List<IMappablePath>,
        renderer: IRenderedPathFactory
    ): Map<Long, RenderedPath> {
        val map = mutableMapOf<Long, RenderedPath>()
        for (path in paths) {
            val lineObj = linePool.get()
            val pathObj = if (shouldRenderWithDrawLines) null else pathPool.get()
            map[path.id] = render(path, renderer, lineObj, pathObj)
        }
        return map
    }

    private suspend fun render(
        path: IMappablePath,
        renderer: IRenderedPathFactory,
        lineObj: MutableList<Float>,
        pathObj: Path?
    ): RenderedPath = onDefault {
        val points = path.points.map { it.coordinate }
        synchronized(lock) {
            lineObj.clear()
            pathObj?.reset()
            renderer.render(points, lineObj)
                .copy(
                    style = path.style,
                    color = path.color,
                    renderedScale = currentScale,
                    path = pathObj?.also {
                        it.drawLines(lineObj.toFloatArray())
                    })
        }
    }

    override fun invalidate() {
        pathsRendered = false
        renderInProgress = false
    }

    override fun onClick(drawer: ICanvasDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun getBounds(drawer: ICanvasDrawer): Rectangle {
        // Rotating by map rotation wasn't working around 90/270 degrees - this is a workaround
        // It will just render slightly more of the path than needed, but never less (since 45 is when the area is at its largest)
        return drawer.getBounds(45f)
    }
}
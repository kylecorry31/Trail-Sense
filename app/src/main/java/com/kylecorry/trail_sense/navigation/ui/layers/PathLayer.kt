package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.coroutines.ControlledRunner
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.*
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.shared.getBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PathLayer : ILayer {

    private var pathsRendered = false
    private var renderInProgress = false
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private val _paths =
        mutableListOf<IMappablePath>() // TODO: Make this Pair<Path, List<PathPoint>>

    private var shouldClip = true
    private var shouldRotateClip = true

    private val lock = Any()

    private val runner = ControlledRunner<Unit>()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setShouldClip(shouldClip: Boolean) {
        this.shouldClip = shouldClip
        invalidate()
    }

    fun setShouldRotateClip(shouldRotateClip: Boolean) {
        this.shouldRotateClip = shouldRotateClip
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
        if (!pathsRendered && !renderInProgress) {
            val renderer = if (shouldClip) {
                ClippedPathRenderer(
                    getBounds(drawer),
                    map::toPixel
                )
            } else {
                PathRenderer(map::toPixel)
            }
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

                pathDrawer.draw(drawer, path.color, strokeScale = scale) {
                    path(path.path)
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
            runner.cancelPreviousThenRun {
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
            renderedPaths.forEach { pathPool.release(it.value.path) }
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
            val pathObj = pathPool.get()
            map[path.id] = render(path, renderer, pathObj)
        }
        return map
    }

    private suspend fun render(
        path: IMappablePath,
        renderer: IRenderedPathFactory,
        pathObj: Path
    ): RenderedPath = onDefault {
        val points = path.points.map { it.coordinate }
        synchronized(lock) {
            pathObj.reset()
            renderer.render(points, pathObj).copy(style = path.style, color = path.color)
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
        return drawer.getBounds(if (shouldRotateClip) 45f else 0f)
    }
}
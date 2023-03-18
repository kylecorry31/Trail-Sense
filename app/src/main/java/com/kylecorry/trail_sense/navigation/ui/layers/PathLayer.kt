package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.*
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.shared.getBounds

class PathLayer : ILayer {

    private var pathsRendered = false
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private val _paths =
        mutableListOf<IMappablePath>() // TODO: Make this Pair<Path, List<PathPoint>>

    private var shouldClip = true
    private var shouldRotateClip = true

    private val lock = Any()

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
        if (!pathsRendered) {
            for (path in renderedPaths) {
                pathPool.release(path.value.path)
            }
            val renderer = if (shouldClip) {
                val bounds = getBounds(drawer)
                ClippedPathRenderer(
                    bounds,
                    map::toPixel
                )
            } else {
                PathRenderer(map::toPixel)
            }
            renderedPaths = generatePaths(_paths, renderer)
            pathsRendered = true
        }

        val factory = PathLineDrawerFactory()
        synchronized(lock) {
            for (path in _paths) {
                val rendered = renderedPaths[path.id] ?: continue
                val pathDrawer = factory.create(path.style)
                val centerPixel = map.toPixel(rendered.origin)
                drawer.push()
                drawer.translate(centerPixel.x, centerPixel.y)

                pathDrawer.draw(drawer, path.color, strokeScale = scale) {
                    path(rendered.path)
                }
                drawer.pop()
            }
        }
        drawer.noStroke()
        drawer.fill(Color.WHITE)
        drawer.noPathEffect()
    }

    private fun generatePaths(
        paths: List<IMappablePath>,
        renderer: IRenderedPathFactory
    ): Map<Long, RenderedPath> {
        val map = mutableMapOf<Long, RenderedPath>()
        synchronized(lock) {
            for (path in paths) {
                val pathObj = pathPool.get()
                pathObj.reset()
                map[path.id] = renderer.render(path.points.map { it.coordinate }, pathObj)
            }
        }
        return map
    }

    override fun invalidate() {
        pathsRendered = false
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
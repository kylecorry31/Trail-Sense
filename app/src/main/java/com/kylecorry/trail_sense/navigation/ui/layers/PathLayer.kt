package com.kylecorry.trail_sense.navigation.ui.layers

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.IRenderedPathFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathLineDrawerFactory
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.PathRenderer
import com.kylecorry.trail_sense.navigation.paths.ui.drawing.RenderedPath
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.shared.maps.ICoordinateToPixelStrategy

class PathLayer : ILayer {

    private var pathsRendered = false
    private var pathPool = ObjectPool { Path() }
    private var renderedPaths = mapOf<Long, RenderedPath>()
    private val _paths =
        mutableListOf<IMappablePath>() // TODO: Make this Pair<Path, List<PathPoint>>

    fun setPaths(paths: List<IMappablePath>) {
        _paths.clear()
        _paths.addAll(paths)
        invalidate()
    }

    override fun draw(drawer: ICanvasDrawer, mapper: ICoordinateToPixelStrategy, scale: Float) {
        if (!pathsRendered) {
            for (path in renderedPaths) {
                pathPool.release(path.value.path)
            }
            renderedPaths = generatePaths(_paths, PathRenderer(mapper))
            pathsRendered = true
        }

        val factory = PathLineDrawerFactory()
        for (path in _paths) {
            val rendered = renderedPaths[path.id] ?: continue
            val pathDrawer = factory.create(path.style)
            val centerPixel = mapper.getPixels(rendered.origin)
            drawer.push()
            drawer.translate(centerPixel.x, centerPixel.y)
            pathDrawer.draw(drawer, path.color, strokeScale = scale) {
                path(rendered.path)
            }
            drawer.pop()
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
        for (path in paths) {
            val pathObj = pathPool.get()
            pathObj.reset()
            map[path.id] = renderer.render(path.points.map { it.coordinate }, pathObj)
        }
        return map
    }

    override fun invalidate() {
        pathsRendered = false
    }
}
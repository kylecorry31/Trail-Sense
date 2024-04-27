package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Path
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.core.cache.ObjectPool
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.extensions.drawLines
import com.kylecorry.trail_sense.shared.getViewBounds
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class ARLineLayer(
    private val renderWithPaths: Boolean = true
) : ARLayer {

    private val lines = mutableListOf<ARLine>()
    private val lineLock = Any()
    private var renderedLines: List<Pair<ARLine, FloatArray>> = emptyList()
    private val pathPool = ObjectPool { Path() }
    private var renderedPaths: List<Pair<ARLine, Path>> = emptyList()
    private val clipper = LineClipper()

    fun setLines(lines: List<ARLine>) {
        synchronized(lineLock) {
            this.lines.clear()
            this.lines.addAll(lines)
        }
    }

    fun clearLines() {
        synchronized(lineLock) {
            lines.clear()
        }
    }


    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val currentLines = synchronized(lineLock) {
            lines.toList()
        }

        val rendered = currentLines.map {
            it to render(it.points.map { it.getAugmentedRealityCoordinate(view) }, view)
        }

        val newPaths = if (renderWithPaths) rendered.map {
            val path = pathPool.get()
            path.reset()
            path.drawLines(it.second)
            it.first to path
        } else {
            emptyList()
        }

        synchronized(lineLock) {
            renderedLines = rendered

            if (renderWithPaths) {
                // Free current paths
                renderedPaths.forEach {
                    pathPool.release(it.second)
                }
                renderedPaths = newPaths
            }
        }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) = synchronized(lineLock) {
        drawer.noFill()
        drawer.strokeCap(StrokeCap.Round)

        // Draw lines
        for (i in renderedLines.indices) {
            val (line, points) = renderedLines[i]
            val path = renderedPaths.getOrNull(i)?.second
            if (line.outlineColor != null) {
                drawer.stroke(line.outlineColor)
                val outlinePx = when (line.thicknessUnits) {
                    ARLine.ThicknessUnits.Dp -> drawer.dp(line.thickness + 2 * line.outlineThickness)
                    ARLine.ThicknessUnits.Angle -> view.sizeToPixel(line.thickness + 2 * line.outlineThickness)
                }
                drawer.strokeWeight(outlinePx)
                if (renderWithPaths && path != null) {
                    drawer.path(path)
                } else {
                    drawer.lines(points)
                }
            }

            drawer.stroke(line.color)
            val thicknessPx = when (line.thicknessUnits) {
                ARLine.ThicknessUnits.Dp -> drawer.dp(line.thickness)
                ARLine.ThicknessUnits.Angle -> view.sizeToPixel(line.thickness)
            }
            drawer.strokeWeight(thicknessPx)
            if (renderWithPaths && path != null) {
                drawer.path(path)
            } else {
                drawer.lines(points)
            }
        }

        drawer.noStroke()
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return false
    }

    private fun render(
        points: List<AugmentedRealityCoordinate>,
        view: AugmentedRealityView
    ): FloatArray {
        val bounds = view.getViewBounds()
        val pixels = points.map { view.toPixel(it) }

        val lines = mutableListOf<Float>()
        clipper.clip(pixels, bounds, lines, preventLineWrapping = true)
        return lines.toFloatArray()
    }

}
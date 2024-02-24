package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView

class ARLineLayer : ARLayer {

    private val lines = mutableListOf<ARLine>()
    private val lineLock = Any()

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

    private var renderedLines: List<Pair<ARLine, FloatArray>> = emptyList()

    override suspend fun update(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        renderedLines = synchronized(lineLock) {
            lines.map {
                it to render(it.points.map { it.getAugmentedRealityCoordinate(view) }, view)
            }
        }
    }

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        drawer.noFill()
        // TODO: Setting the stroke cap to round causes artifacts between lines, but the end looks good - setting it to project fixes the artifacts but the end looks bad
        drawer.strokeCap(StrokeCap.Round)

        val rendered = synchronized(lineLock) {
            renderedLines.toList()
        }

        // Draw lines
        for ((line, points) in rendered) {
            if (line.outlineColor != null) {
                drawer.stroke(line.outlineColor)
                val outlinePx = when (line.thicknessUnits) {
                    ARLine.ThicknessUnits.Dp -> drawer.dp(line.thickness + 2 * line.outlineThickness)
                    ARLine.ThicknessUnits.Angle -> view.sizeToPixel(line.thickness + 2 * line.outlineThickness)
                }
                drawer.strokeWeight(outlinePx)
                drawer.lines(points)
            }

            drawer.stroke(line.color)
            val thicknessPx = when (line.thicknessUnits) {
                ARLine.ThicknessUnits.Dp -> drawer.dp(line.thickness)
                ARLine.ThicknessUnits.Angle -> view.sizeToPixel(line.thickness)
            }
            drawer.strokeWeight(thicknessPx)
            drawer.lines(points)
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
        val bounds = view.getBounds()
        val pixels = points.map { view.toPixel(it) }

        val lines = mutableListOf<Float>()
        clipper.clip(pixels, bounds, lines, preventLineWrapping = true)
        return lines.toFloatArray()
    }

}
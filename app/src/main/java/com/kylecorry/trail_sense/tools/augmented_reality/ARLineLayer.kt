package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.LineClipper
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.tools.augmented_reality.position.AugmentedRealityCoordinate

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

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        drawer.noFill()
        // TODO: Setting the stroke cap to round causes artifacts between lines, but the end looks good - setting it to project fixes the artifacts but the end looks bad
        drawer.strokeCap(StrokeCap.Round)

        val lines = synchronized(lineLock) {
            lines.toList()
        }

        // Draw lines
        for (line in lines) {
            drawer.stroke(line.color)
            val thicknessPx = when (line.thicknessUnits) {
                ARLine.ThicknessUnits.Dp -> drawer.dp(line.thickness)
                ARLine.ThicknessUnits.Angle -> view.sizeToPixel(line.thickness)
            }
            drawer.strokeWeight(thicknessPx)

            render(line.points.map { it.getAugmentedRealityCoordinate(view) }, view, drawer)
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
        view: AugmentedRealityView,
        drawer: ICanvasDrawer
    ) {
        val bounds = view.getBounds()
        val pixels = points.map { view.toPixel(it) }

        val lines = mutableListOf<Float>()
        clipper.clip(pixels, bounds, lines, preventLineWrapping = true)
        drawer.lines(lines.toFloatArray())
    }

}
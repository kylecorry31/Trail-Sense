package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.shared.extensions.isSamePixel
import com.kylecorry.trail_sense.shared.getBounds
import com.kylecorry.trail_sense.shared.toPixelCoordinate
import com.kylecorry.trail_sense.shared.toVector2
import com.kylecorry.trail_sense.tools.augmented_reality.position.AugmentedRealityCoordinate

class ARLineLayer : ARLayer {

    private val lines = mutableListOf<ARLine>()
    private val lineLock = Any()

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
        var previous: PixelCoordinate? = null

        val multiplier = 1.5f

        val minX = view.width * -multiplier
        val maxX = view.width * (1 + multiplier)
        val minY = view.height * -multiplier
        val maxY = view.height * (1 + multiplier)

        val lines = mutableListOf<Float>()

        for (pixel in pixels) {
            // Remove lines that cross the entire screen (because they are behind the camera)
            val isLineInvalid = previous != null &&
                    (pixel.x < minX && previous.x > maxX ||
                            pixel.x > maxX && previous.x < minX ||
                            pixel.y < minY && previous.y > maxY ||
                            pixel.y > maxY && previous.y < minY)

            if (previous != null && !isLineInvalid) {
                // If the end point is the same as the previous, don't draw a line
                if (previous.isSamePixel(pixel)) {
                    continue
                }
                addLine(bounds, previous, pixel, lines)
            }
            previous = pixel
        }

        drawer.lines(lines.toFloatArray())
    }

    // TODO: Extract this
    private fun addLine(
        bounds: Rectangle,
        start: PixelCoordinate,
        end: PixelCoordinate,
        lines: MutableList<Float>
    ) {
        val a = start.toVector2(bounds.top)
        val b = end.toVector2(bounds.top)

        // Both are in
        if (bounds.contains(a) && bounds.contains(b)) {
            lines.add(start.x)
            lines.add(start.y)
            lines.add(end.x)
            lines.add(end.y)
            return
        }

        val intersection =
            Geometry.getIntersection(a, b, bounds).map { it.toPixelCoordinate(bounds.top) }

        // A is in, B is not
        if (bounds.contains(a)) {
            if (intersection.any()) {
                lines.add(start.x)
                lines.add(start.y)
                lines.add(intersection[0].x)
                lines.add(intersection[0].y)
            }
            return
        }

        // B is in, A is not
        if (bounds.contains(b)) {
            if (intersection.any()) {
                lines.add(intersection[0].x)
                lines.add(intersection[0].y)
                lines.add(end.x)
                lines.add(end.y)
            }
            return
        }

        // Both are out, but may intersect
        if (intersection.size == 2) {
            lines.add(intersection[0].x)
            lines.add(intersection[0].y)
            lines.add(intersection[1].x)
            lines.add(intersection[1].y)
        }
    }

}
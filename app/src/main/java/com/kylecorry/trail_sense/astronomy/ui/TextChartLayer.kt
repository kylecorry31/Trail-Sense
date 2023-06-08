package com.kylecorry.trail_sense.astronomy.ui

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.ceres.chart.IChart
import com.kylecorry.ceres.chart.data.ChartLayer
import com.kylecorry.sol.math.Vector2

class TextChartLayer(
    initialText: String,
    initialPosition: Vector2,
    @ColorInt initialColor: Int,
    initialSize: Float = 12f,
    private val verticalAlign: TextVerticalPosition = TextVerticalPosition.Center,
    private val horizontalAlign: TextHorizontalPosition = TextHorizontalPosition.Center
) : ChartLayer {

    @ColorInt
    var color: Int = initialColor
        set(value) {
            field = value
            invalidate()
        }

    var position: Vector2 = initialPosition
        set(value) {
            field = value
            invalidate()
        }

    var text: String = initialText
        set(value) {
            field = value
            invalidate()
        }

    var size: Float = initialSize
        set(value) {
            field = value
            invalidate()
        }

    override var data: List<Vector2> = emptyList()

    override var hasChanges: Boolean = false
        private set

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        val chartPosition = chart.toPixel(position)

        drawer.textSize(drawer.sp(size))
        drawer.noStroke()
        drawer.fill(color)
        drawer.textMode(TextMode.Corner)
        val tWidth = drawer.textWidth(text)
        val tHeight = drawer.textHeight(text)

        val x = when (horizontalAlign) {
            TextHorizontalPosition.Left -> chartPosition.x
            TextHorizontalPosition.Center -> chartPosition.x - tWidth / 2
            TextHorizontalPosition.Right -> chartPosition.x - tWidth
        }

        val y = when (verticalAlign) {
            TextVerticalPosition.Top -> chartPosition.y + tHeight
            TextVerticalPosition.Center -> chartPosition.y + tHeight / 2
            TextVerticalPosition.Bottom -> chartPosition.y
        }

        drawer.text(text, x, y)

        // Reset the opacity
        drawer.opacity(255)

        hasChanges = false
    }

    override fun onClick(drawer: ICanvasDrawer, chart: IChart, pixel: PixelCoordinate): Boolean {
        return false
    }

    override fun invalidate() {
        hasChanges = true
    }

    enum class TextVerticalPosition {
        Top, Center, Bottom
    }

    enum class TextHorizontalPosition {
        Left, Center, Right
    }

}
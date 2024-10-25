package com.kylecorry.trail_sense.tools.astronomy.ui

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.views.chart.IChart
import com.kylecorry.andromeda.views.chart.data.BaseChartLayer
import com.kylecorry.sol.math.Vector2

class BitmapChartLayer2(
    initialData: List<Vector2>,
    initialBitmap: Bitmap,
    private val size: Float = 12f,
    @ColorInt initialTint: Int? = null,
    initialRotation: Float = 0f,
    onPointClick: (point: Vector2) -> Boolean = { false }
) : BaseChartLayer(initialData, true, size, onPointClick) {


    @ColorInt
    var tint: Int? = initialTint
        set(value) {
            field = value
            invalidate()
        }

    var rotation: Float = initialRotation
        set(value) {
            field = value
            invalidate()
        }

    var bitmap: Bitmap = initialBitmap
        set(value) {
            field = value
            invalidate()
        }

    override fun draw(drawer: ICanvasDrawer, chart: IChart) {
        drawer.opacity(255)
        drawer.noStroke()
        drawer.noFill()
        val tint = tint
        if (tint != null) {
            drawer.tint(tint)
        } else {
            drawer.noTint()
        }
        drawer.imageMode(ImageMode.Center)
        val dpSize = drawer.dp(size)
        for (point in data) {
            val mapped = chart.toPixel(point)
            drawer.push()
            drawer.translate(mapped.x, mapped.y)
            drawer.rotate(rotation, 0f, 0f)
            drawer.image(bitmap, 0f, 0f, dpSize, dpSize)
            drawer.pop()
        }
        super.draw(drawer, chart)
    }
}
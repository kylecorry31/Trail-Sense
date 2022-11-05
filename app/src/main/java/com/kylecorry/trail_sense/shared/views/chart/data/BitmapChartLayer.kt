package com.kylecorry.trail_sense.shared.views.chart.data

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.sol.math.Vector2
import com.kylecorry.trail_sense.shared.views.chart.IChart

class BitmapChartLayer(
    initialData: List<Vector2>,
    initialBitmap: Bitmap,
    private val size: Float = 12f,
    @ColorInt initialTint: Int? = null,
    onPointClick: (point: Vector2) -> Boolean = { false }
) : BaseChartLayer(initialData, true, size, onPointClick) {


    @ColorInt
    var tint: Int? = initialTint
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
            drawer.image(bitmap, mapped.x, mapped.y, dpSize, dpSize)
        }
        super.draw(drawer, chart)
    }
}
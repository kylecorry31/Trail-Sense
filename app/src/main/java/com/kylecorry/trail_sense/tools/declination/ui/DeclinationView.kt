package com.kylecorry.trail_sense.tools.declination.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class DeclinationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CanvasView(context, attrs) {

    private var color: Int = Color.BLACK
    private lateinit var starBitmap: Bitmap
    private var imageSize: Float = 0f

    init {
        runEveryCycle = false
    }

    var declination: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun setup() {
        color = Resources.androidTextColorPrimary(context)
        imageSize = drawer.dp(24f)
        starBitmap = drawer.loadImage(R.drawable.ic_star, imageSize.toInt(), imageSize.toInt())
    }

    override fun draw() {
        drawer.clear()

        drawer.stroke(color)
        drawer.strokeWeight(imageSize / 4f)
        drawer.strokeCap(StrokeCap.Square)

        drawer.tint(color)

        // True North arrow
        drawer.line(
            drawer.canvas.width / 2f,
            drawer.canvas.height.toFloat(),
            drawer.canvas.width / 2f,
            imageSize * 1.25f
        )

        drawer.imageMode(ImageMode.Center)
        drawer.image(
            starBitmap,
            drawer.canvas.width / 2f,
            imageSize * 0.5f
        )

        // Declination arrow
        drawer.push()
        drawer.rotate(declination, drawer.canvas.width / 2f, drawer.canvas.height.toFloat())

        drawer.line(
            drawer.canvas.width / 2f,
            drawer.canvas.height.toFloat(),
            drawer.canvas.width / 2f,
            imageSize * 1.25f
        )

        // TODO: Draw arrow head

        drawer.pop()
    }
}
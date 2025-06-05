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

        val strokeWeight = imageSize / 4f
        val offsetFromTop = imageSize * 1.25f
        val arrowVerticalOffset = imageSize * 2.25f
        val arrowHorizontalOffset = imageSize / 2

        drawer.clear()

        drawer.stroke(color)
        drawer.strokeWeight(strokeWeight)
        drawer.strokeCap(StrokeCap.Round)

        drawer.tint(color)

        // True North arrow
        drawer.line(
            drawer.canvas.width / 2f,
            drawer.canvas.height.toFloat() - strokeWeight,
            drawer.canvas.width / 2f,
            offsetFromTop
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

        val line = mutableListOf(
            // Vertical
            drawer.canvas.width / 2f,
            drawer.canvas.height.toFloat() - strokeWeight,
            drawer.canvas.width / 2f,
            offsetFromTop
        )

        if (declination <= 0f) {
            line.add(drawer.canvas.width / 2f - arrowHorizontalOffset)
            line.add(arrowVerticalOffset)
            line.add(drawer.canvas.width / 2f)
            line.add(offsetFromTop)
        }

        if (declination >= 0f) {
            line.add(drawer.canvas.width / 2f + arrowHorizontalOffset)
            line.add(arrowVerticalOffset)
            line.add(drawer.canvas.width / 2f)
            line.add(offsetFromTop)
        }

        drawer.lines(line.toFloatArray())

        drawer.pop()
    }
}
package com.kylecorry.trail_sense.tools.maps.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.math.map
import kotlin.math.*

class MagnetometerView: CanvasView {

    private var magneticField: FloatArray = floatArrayOf(0f, 0f, 0f)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
    }

    override fun setup() {
    }

    override fun draw() {
        background(Color.TRANSPARENT)
        stroke(Color.WHITE)
        strokeWeight(4f)
        noFill()
        circle(width / 2f, height / 2f, width * 0.75f)
        noStroke()
        if (magneticField[0].absoluteValue > 3f) {
            fill(Color.GREEN)
            circle(width / 2f + getPosition(magneticField[0]), height / 2f, 20f)
        }

        if (magneticField[1].absoluteValue > 3f) {
            fill(Color.BLUE)
            circle(width / 2f, height / 2f - getPosition(magneticField[1]), 20f)
        }

        if (magneticField[0].absoluteValue > 3f && magneticField[1].absoluteValue > 3f){
            val angle = atan2(magneticField[1], magneticField[0])
            val magnitude = sqrt(magneticField[0] * magneticField[0] + magneticField[1] * magneticField[1])
            val x = cos(angle) * width / 2f * 0.75f //getPosition(magnitude)
            val y = sin(angle) * width / 2f * 0.75f // getPosition(magnitude)

            fill(Color.YELLOW)
            circle(width / 2f + x, height / 2f - y, 30f)

        }

    }

    private fun getPosition(magnitude: Float): Float {
        if (magnitude == 0f){
            return width / 2f * 0.75f
        }
        return width / 2f * 0.75f * map(1 / magnitude, 0f, 1 / 3f, 0f, 1f)
    }

    fun setMagneticField(field: FloatArray){
        magneticField = field.clone()
        invalidate()
    }
}
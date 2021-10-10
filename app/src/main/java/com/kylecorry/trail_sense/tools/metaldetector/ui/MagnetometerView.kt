package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.sol.units.Bearing
import kotlin.math.min

class MagnetometerView : CanvasView {

    private var fieldStrength = 0f
    private var direction = Bearing(0f) to Bearing(180f)
    private var radius = 0f
    private var indicatorSize = 0f
    private var singlePole = false
    private var sensitivity = 1f

    private val formatService by lazy { FormatService(context) }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        runEveryCycle = false
        setupAfterVisible = true
    }

    override fun setup() {
        radius = min(height / 2f * 0.75f, width / 2f * 0.75f)
        textMode(TextMode.Center)
        textSize(sp(18f))
        indicatorSize = dp(20f)
    }

    override fun draw() {
        background(Color.TRANSPARENT)
        stroke(Color.WHITE)
        strokeWeight(4f)
        noFill()
        circle(width / 2f, height / 2f, radius * 2)
        noStroke()

        fill(Color.WHITE)
        text(formatService.formatMagneticField(fieldStrength), width / 2f, height / 2f)

        if (fieldStrength < sensitivity) {
            return
        }

        push()
        rotate(-direction.first.value)

        if (singlePole) {
            fill(AppColor.Green.color)
            if (direction.first.value in 90f..270f) {
                rotate(180f)
            }
            circle(width / 2f, height / 2f - radius, indicatorSize)
        } else {
            fill(AppColor.Red.color)
            circle(width / 2f, height / 2f - radius, indicatorSize)

            rotate(180f)
            fill(AppColor.Blue.color)
            circle(width / 2f, height / 2f - radius, indicatorSize)
        }
        pop()
    }

    fun setFieldStrength(strength: Float) {
        fieldStrength = strength
        invalidate()
    }

    fun setMetalDirection(direction: Pair<Bearing, Bearing>) {
        this.direction = direction
        invalidate()
    }

    fun setSinglePoleMode(singlePole: Boolean) {
        this.singlePole = singlePole
        invalidate()
    }

    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity
        invalidate()
    }


}
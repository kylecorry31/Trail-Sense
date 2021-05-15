package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.metaldetection.MetalDetectionService
import kotlin.math.*

class MagnetometerView : CanvasView {

    private var magneticField = Vector3.zero
    private var lastNonMetal = Vector3.zero
    private var threshold = 0f
    private var radius = 0f

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
        radius = min(height / 2f * 0.75f, width / 2f * 0.75f)
    }

    override fun draw() {
        background(Color.TRANSPARENT)
        stroke(Color.WHITE)
        strokeWeight(4f)
        noFill()
        circle(width / 2f, height / 2f, radius * 2)
        noStroke()

        val isMetal = MetalDetectionService().isMetal(
            magneticField, threshold
        )

        if (!isMetal) {
            lastNonMetal = magneticField.copy()
            return
        }

        val calibrated = magneticField - lastNonMetal

        val angle = atan2(calibrated.y, calibrated.x)
        // TODO: Blink based on distance
        val magnitude = calibrated.magnitude()
        val x = cos(angle) * radius
        val y = sin(angle) * radius

        fill(Color.YELLOW)
        circle(width / 2f + x, height / 2f - y, 30f)
    }

    fun setMagneticField(field: Vector3, threshold: Float) {
        magneticField = field.copy()
        this.threshold = threshold
        invalidate()
    }
}
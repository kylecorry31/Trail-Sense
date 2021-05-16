package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.math.Vector3
import kotlin.math.*

class MagnetometerView : CanvasView {

    private var magneticField = Vector3.zero
    private var geomagneticField = Vector3.zero
    private var radius = 0f
    private var indicatorSize = 0f

    private val formatService by lazy { FormatServiceV2(context) }

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

        val calibrated = magneticField - geomagneticField

        val magnitude = calibrated.magnitude()

        fill(Color.WHITE)
        text(formatService.formatMagneticField(magnitude), width / 2f, height / 2f)

        if (magnitude < 1f){
            return
        }

        val angle = atan2(calibrated.y, calibrated.x)
        val x = cos(angle) * radius
        val y = sin(angle) * radius

        fill(AppColor.Green.color)
        circle(width / 2f + x, height / 2f - y, indicatorSize)
    }

    fun setMagneticField(field: Vector3) {
        magneticField = field.copy()
        invalidate()
    }

    fun setGeomagneticField(field: Vector3){
        geomagneticField = field
        invalidate()
    }
}
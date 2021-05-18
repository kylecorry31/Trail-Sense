package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.math.Vector3
import kotlin.math.*

class MagnetometerView : CanvasView {

    private var magneticField = Vector3.zero
    private var geomagneticField = Vector3.zero
    private var gravity = Vector3.zero
    private var radius = 0f
    private var indicatorSize = 0f
    private var singlePole = false
    private var sensitivity = 1f

    private val geoService = GeoService()
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

        val calibrated =
            magneticField - geomagneticField // magneticField - magneticField.normalize() * geomagneticField.magnitude()

        val magnitude = calibrated.magnitude()

        fill(Color.WHITE)
        text(formatService.formatMagneticField(magnitude), width / 2f, height / 2f)

        if (magnitude < sensitivity) {
            return
        }

        val azimuth = geoService.getAzimuth(gravity, calibrated)?.value ?: return

        push()
        rotate(-azimuth)

        if (singlePole) {
            fill(AppColor.Green.color)
            if (azimuth in 90f..270f) {
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

    fun setMagneticField(field: Vector3) {
        magneticField = field.copy()
        invalidate()
    }

    fun setGeomagneticField(field: Vector3) {
        geomagneticField = field
        invalidate()
    }

    fun setGravity(vec: Vector3) {
        gravity = vec
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
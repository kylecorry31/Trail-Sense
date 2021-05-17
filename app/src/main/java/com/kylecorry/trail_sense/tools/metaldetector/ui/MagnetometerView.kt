package com.kylecorry.trail_sense.tools.metaldetector.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.views.CanvasView
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.math.Vector3
import com.kylecorry.trailsensecore.domain.math.toDegrees
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.VectorCompass
import kotlin.math.*

class MagnetometerView : CanvasView {

    private var magneticField = Vector3.zero
    private var geomagneticField = Vector3.zero
    private var gravity = Vector3.zero
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

        // TODO: Measure change in position, if magnitude increasing and position increasing Y, then choose point closest (and opposite)
        // TODO: Measure change in position, if magnitude increasing and position increasing X, then choose point closest (and opposite)
        val azimuth = AzimuthCalculator.calculate(gravity, calibrated)?.value ?: return

        push()
        rotate(-azimuth)

        fill(AppColor.Red.color)
        circle(width / 2f, height / 2f - radius, indicatorSize)

        rotate(180f)
        fill(AppColor.Blue.color)
        circle(width / 2f, height / 2f - radius, indicatorSize)
        pop()
    }

    fun setMagneticField(field: Vector3) {
        magneticField = field.copy()
        invalidate()
    }

    fun setGeomagneticField(field: Vector3){
        geomagneticField = field
        invalidate()
    }

    fun setGravity(vec: Vector3){
        gravity = vec
        invalidate()
    }


}


object AzimuthCalculator {

    fun calculate(gravity: FloatArray, magneticField: FloatArray): Bearing? {
        // Gravity
        val normGravity = Vector3Utils.normalize(gravity)
        val normMagField = Vector3Utils.normalize(magneticField)

        // East vector
        val east = Vector3Utils.cross(normMagField, normGravity)
        val normEast = Vector3Utils.normalize(east)

        // Magnitude check
        val eastMagnitude = Vector3Utils.magnitude(east)
        val gravityMagnitude = Vector3Utils.magnitude(gravity)
        val magneticMagnitude = Vector3Utils.magnitude(magneticField)
        if (gravityMagnitude * magneticMagnitude * eastMagnitude < 0.1f) {
            return null
        }

        // North vector
        val dotProduct = Vector3Utils.dot(normGravity, normMagField)
        val north = Vector3Utils.minus(normMagField, Vector3Utils.times(normGravity, dotProduct))
        val normNorth = Vector3Utils.normalize(north)

        // Azimuth
        // NB: see https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
        val sin = normEast[1] - normNorth[0]
        val cos = normEast[0] + normNorth[1]
        val azimuth = if (!(sin == 0f && sin == cos)) atan2(sin, cos) else 0f

        if (azimuth.isNaN()){
            return null
        }

        return Bearing(azimuth.toDegrees())
    }


    fun calculate(gravity: Vector3, magneticField: Vector3): Bearing? {
        return calculate(gravity.toFloatArray(), magneticField.toFloatArray())
    }

}

object Vector3Utils {
    fun cross(first: FloatArray, second: FloatArray): FloatArray {
        return floatArrayOf(
            first[1] * second[2] - first[2] * second[1],
            first[2] * second[0] - first[0] * second[2],
            first[0] * second[1] - first[1] * second[0]
        )
    }

    fun minus(first: FloatArray, second: FloatArray): FloatArray {
        return floatArrayOf(
            first[0] - second[0],
            first[1] - second[1],
            first[2] - second[2]
        )
    }

    fun plus(first: FloatArray, second: FloatArray): FloatArray {
        return floatArrayOf(
            first[0] + second[0],
            first[1] + second[1],
            first[2] + second[2]
        )
    }

    fun times(arr: FloatArray, factor: Float): FloatArray {
        return floatArrayOf(
            arr[0] * factor,
            arr[1] * factor,
            arr[2] * factor
        )
    }

    fun dot(first: FloatArray, second: FloatArray): Float {
        return first[0] * second[0] + first[1] * second[1] + first[2] * second[2]
    }

    fun magnitude(arr: FloatArray): Float {
        return sqrt(arr[0] * arr[0] + arr[1] * arr[1] + arr[2] * arr[2])
    }

    fun normalize(arr: FloatArray): FloatArray {
        val mag = magnitude(arr)
        return floatArrayOf(
            arr[0] / mag,
            arr[1] / mag,
            arr[2] / mag
        )
    }
}
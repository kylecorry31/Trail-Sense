package com.kylecorry.trail_sense.tools.augmented_reality

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.hardware.SensorManager
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.clinometer.CameraClinometer
import com.kylecorry.andromeda.sense.clinometer.SideClinometer
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.QuaternionMath
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.SolMath.toRadians
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text
import com.kylecorry.trail_sense.shared.textDimensions
import java.time.Duration
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// TODO: Notify location change
// TODO: This needs a parent view that has the camera, this, and any buttons (like the freeform button)
class AugmentedRealityView : CanvasView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var fov: Size = Size(45f, 45f)

    var focusText: String? = null

    var backgroundFillColor: Int = Color.TRANSPARENT

    // TODO: Remove legacy orientation in favor of a custom orientation sensor
    private var legacyOrientation = Quaternion.zero
    private var rotationMatrix = FloatArray(16)
    private val quaternion = FloatArray(4)
    private val tempRotationResult = FloatArray(4)
    private val tempWorldVector = FloatArray(4)


    // Sensors / preferences
    private val userPrefs = UserPreferences(context)
    private val sensors = SensorService(context)
    private val compass = sensors.getCompass()
    private val clinometer = CameraClinometer(context)
    private val sideClinometer = SideClinometer(context)
    private val orientationSensor = sensors.getOrientation()
    private val gps = sensors.getGPS(frequency = Duration.ofMillis(200))
    private val altimeter = sensors.getAltimeter(gps = gps)
    private val declinationProvider = DeclinationFactory().getDeclinationStrategy(
        userPrefs,
        gps
    )
    private val isTrueNorth = userPrefs.compass.useTrueNorth

    private val orientationLock = Any()

    /**
     * The compass bearing of the device in degrees.
     */
    var azimuth = 0f
        private set

    /**
     * The angle of the device from top to bottom in degrees.
     */
    var inclination = 0f
        private set

    /**
     * The angle of the device from side to side in degrees
     */
    var sideInclination = 0f
        private set

    /**
     * The location of the device
     */
    val location: Coordinate
        get() = gps.location

    /**
     * The altitude of the device in meters
     */
    val altitude: Float
        get() = altimeter.altitude

    /**
     * The diameter of the reticle in pixels
     */
    val reticleDiameter: Float
        get() = dp(36f)

    private val layers = mutableListOf<ARLayer>()
    private val layerLock = Any()

    // Guidance
    private var guideLocation: ARPosition? = null
    private var guideThreshold: Float? = null
    private var onGuideReached: (() -> Unit)? = null

    fun start() {
        gps.start(this::onSensorUpdate)
        altimeter.start(this::onSensorUpdate)
        if (orientationSensor != null) {
            orientationSensor.start(this::onSensorUpdate)
        } else {
            compass.start(this::onSensorUpdate)
            clinometer.start(this::onSensorUpdate)
            sideClinometer.start(this::onSensorUpdate)
        }
    }

    fun stop() {
        gps.stop(this::onSensorUpdate)
        altimeter.stop(this::onSensorUpdate)
        if (orientationSensor != null) {
            orientationSensor.stop(this::onSensorUpdate)
        } else {
            compass.stop(this::onSensorUpdate)
            clinometer.stop(this::onSensorUpdate)
            sideClinometer.stop(this::onSensorUpdate)
        }
    }

    fun addLayer(layer: ARLayer) {
        synchronized(layerLock) {
            layers.add(layer)
        }
    }

    fun removeLayer(layer: ARLayer) {
        synchronized(layerLock) {
            layers.remove(layer)
        }
    }

    fun clearLayers() {
        synchronized(layerLock) {
            layers.clear()
        }
    }

    fun setLayers(layers: List<ARLayer>) {
        synchronized(layerLock) {
            this.layers.clear()
            this.layers.addAll(layers)
        }
    }

    fun guideTo(
        location: ARPosition,
        thresholdDegrees: Float? = null,
        onReached: () -> Unit = { clearGuide() }
    ) {
        guideLocation = location
        guideThreshold = thresholdDegrees
        onGuideReached = onReached
    }

    fun clearGuide() {
        guideLocation = null
        guideThreshold = null
        onGuideReached = null
    }

    private fun onSensorUpdate(): Boolean {
        if (orientationSensor != null) {
            // No need to update the positions because the orientation sensor is valid
            return true
        }
        if (isTrueNorth) {
            compass.declination = declinationProvider.getDeclination()
        } else {
            compass.declination = 0f
        }
        synchronized(orientationLock) {
            azimuth = compass.rawBearing
            inclination = clinometer.incline
            sideInclination = sideClinometer.angle - 90f
        }
        return true
    }


    override fun setup() {
        updateOrientation()
    }

    override fun draw() {
        updateOrientation()
        background(backgroundFillColor)

        layers.forEach {
            it.draw(this, this)
        }

        var hasFocus = false
        for (layer in layers.reversed()) {
            if (layer.onFocus(this, this)) {
                hasFocus = true
                break
            }
        }

        // TODO: Should the onFocus method just return a string?
        if (!hasFocus) {
            focusText = null
        }

        drawReticle()
        drawGuidance()
        drawFocusText()
    }

    private fun drawGuidance() {
        // Draw an arrow around the reticle that points to the desired location
        val coordinate = guideLocation?.getHorizonCoordinate(location, altitude) ?: return
        val threshold = guideThreshold
        val point = toPixel(coordinate)
        val center = PixelCoordinate(width / 2f, height / 2f)
        val reticle = PixelCircle(center, reticleDiameter / 2f)
        val circle = PixelCircle(
            point,
            if (threshold == null) reticleDiameter / 2f else sizeToPixel(threshold)
        )

        if (circle.contains(center)) {
            onGuideReached?.invoke()
        }

        if (reticle.contains(point)) {
            // No need to draw the arrow - it's already centered
            return
        }

        val angle = atan2(point.y - center.y, point.x - center.x).toDegrees()
        push()
        rotate(angle, center.x, center.y)
        val size = drawer.dp(16f)
        translate(center.x + reticleDiameter / 2f + size / 2f + dp(4f), center.y)
        rotate(90f, 0f, 0f)
        // Draw an arrow
        val path = Path()

        // Bottom Left
        path.moveTo(-size / 2.5f, size / 2f)

        // Top
        path.lineTo(0f, -size / 2f)

        // Bottom right
        path.lineTo(size / 2.5f, size / 2f)

        // Middle dip
        path.lineTo(0f, size / 3f)

        path.close()
        noStroke()
        fill(Color.WHITE.withAlpha(127))
        path(path)
        pop()
    }

    private fun drawFocusText() {
        val textToRender = focusText ?: return
        if (textToRender.isBlank()) return

        // Background
        noStroke()
        fill(Color.BLACK.withAlpha(127))
        val totalDimensions = textDimensions(textToRender, dp(4f))
        val padding = dp(8f)
        rect(
            width / 2f - totalDimensions.first / 2f - padding,
            height / 2f + reticleDiameter / 2f + dp(4f) + padding,
            totalDimensions.first + padding * 2,
            totalDimensions.second + padding * 2,
            dp(4f)
        )


        fill(Color.WHITE)
        textSize(drawer.sp(16f))
        textMode(TextMode.Corner)
        textAlign(TextAlign.Center)

        // TODO: Save the dp values
        text(
            textToRender,
            width / 2f,
            height / 2f + reticleDiameter / 2f + dp(24f) + padding,
            dp(4f)
        )
    }

    private fun drawReticle() {
        stroke(Color.WHITE.withAlpha(127))
        strokeWeight(dp(2f))
        noFill()
        circle(width / 2f, height / 2f, reticleDiameter)
    }

    private fun toWorldSpace(bearing: Float, elevation: Float, distance: Float): Vector3 {
        val thetaRad = elevation.toRadians()
        val phiRad = bearing.toRadians()

        val cosTheta = cos(thetaRad)
        val x = distance * cosTheta * sin(phiRad)
        val y = distance * cosTheta * cos(phiRad)
        val z = distance * sin(thetaRad)
        return Vector3(x, y, z)
    }

    private fun applyRotation(vector: Vector3): Vector3 {
        if (orientationSensor == null){
            return legacyOrientation.rotate(vector)
        }
        tempWorldVector[0] = vector.x
        tempWorldVector[1] = vector.y
        tempWorldVector[2] = vector.z
        tempWorldVector[3] = 1f
        Matrix.multiplyMV(tempRotationResult, 0, rotationMatrix, 0, tempWorldVector, 0)
        return Vector3(tempRotationResult[0], tempRotationResult[1], tempRotationResult[2])
    }

    private fun toSpherical(vector: Vector3): Vector3 {
        val r = vector.magnitude()
        val theta = asin(vector.z / r).toDegrees().real(0f)
        val phi = atan2(vector.x, vector.y).toDegrees().real(0f)
        return Vector3(r, theta, phi)
    }

    /**
     * Converts an angular size to a pixel size
     * @param angularSize The angular size in degrees
     * @return The pixel size
     */
    fun sizeToPixel(angularSize: Float): Float {
        return (width / fov.width) * angularSize
    }

    fun sizeToPixel(diameter: Distance, distance: Distance): Float {
        val angularSize = AugmentedRealityUtils.getAngularSize(diameter, distance)
        return sizeToPixel(angularSize)
    }

    // TODO: These are off by a about a degree when you point the device at around 45 degrees (ex. a north line appears 1 degree to the side of actual north)
    // TODO: This may just be the compass being off
    /**
     * Gets the pixel coordinate of a point on the screen given the bearing and azimuth.
     * @param coordinate The horizon coordinate of the point
     * @return The pixel coordinate of the point
     */
    fun toPixel(coordinate: HorizonCoordinate): PixelCoordinate {
        val bearing = getActualBearing(coordinate)

        val world = toWorldSpace(bearing, coordinate.elevation, 1f)
        val rotated = applyRotation(world)
        val spherical = toSpherical(rotated)

        // TODO: Try out Matrix.perspectiveM

        // The rotation of the device has been negated, so azimuth = 0 and inclination = 0 is used
        return AugmentedRealityUtils.getPixelLinear(
            spherical.z,
            0f,
            spherical.y,
            0f,
            Size(width.toFloat(), height.toFloat()),
            fov
        )
    }

    fun toPixel(coordinate: Coordinate, elevation: Float? = null): PixelCoordinate {
        val bearing = gps.location.bearingTo(coordinate).value
        val distance = gps.location.distanceTo(coordinate)
        val elevationAngle = if (elevation == null) {
            0f
        } else {
            atan2((elevation - gps.altitude), distance).toDegrees()
        }
        return toPixel(HorizonCoordinate(bearing, elevationAngle, true))
    }

    private fun getActualBearing(coordinate: HorizonCoordinate): Float {
        return if (isTrueNorth && !coordinate.isTrueNorth) {
            // Convert coordinate to true north
            DeclinationUtils.toTrueNorthBearing(
                coordinate.bearing,
                declinationProvider.getDeclination()
            )
        } else if (!isTrueNorth && coordinate.isTrueNorth) {
            // Convert coordinate to magnetic north
            DeclinationUtils.fromTrueNorthBearing(
                coordinate.bearing,
                declinationProvider.getDeclination()
            )
        } else {
            coordinate.bearing
        }
    }

    private fun updateOrientation() {
        if (orientationSensor == null){
            // TODO: This fails when the device is pointed almost straight up or down
            legacyOrientation = Quaternion.from(Euler(inclination, -sideInclination, -azimuth)).inverse()
            return
        }


        // Convert the orientation a rotation matrix
        QuaternionMath.inverse(orientationSensor.rawOrientation, quaternion)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, quaternion)

        // Remap the coordinate system to AR space
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            rotationMatrix
        )

        // Add declination
        if (isTrueNorth) {
            val declination = declinationProvider.getDeclination()
            Matrix.rotateM(rotationMatrix, 0, declination, 0f, 0f, 1f)
        }

        // Get orientation from rotation matrix
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        azimuth = orientation[0].toDegrees()
        inclination = -orientation[1].toDegrees()
        sideInclination = -orientation[2].toDegrees()
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pixel = PixelCoordinate(e.x, e.y)
            for (layer in layers.reversed()) {
                if (layer.onClick(this@AugmentedRealityView, this@AugmentedRealityView, pixel)) {
                    return true
                }
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private val mGestureDetector = GestureDetector(context, gestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        invalidate()
        return true
    }

    data class HorizonCoordinate(
        val bearing: Float,
        val elevation: Float,
        val isTrueNorth: Boolean = true
    )

    data class Point(
        val coordinate: HorizonCoordinate,
        val size: Float,
        val color: Int,
        val text: String? = null
    )

}
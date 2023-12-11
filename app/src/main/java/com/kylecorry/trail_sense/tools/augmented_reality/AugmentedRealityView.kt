package com.kylecorry.trail_sense.tools.augmented_reality

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text
import com.kylecorry.trail_sense.shared.textDimensions
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.position.ARPoint
import java.time.Duration
import kotlin.math.atan2

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

    private var rotationMatrix = FloatArray(16)
    private val orientation = FloatArray(3)
    private var size = Size(width.toFloat(), height.toFloat())

    // Sensors / preferences
    private val userPrefs = UserPreferences(context)
    private val sensors = SensorService(context)
    private var orientationSensor: IOrientationSensor = sensors.getOrientation()
    private val gps = sensors.getGPS(frequency = Duration.ofMillis(200))
    private val altimeter = sensors.getAltimeter(gps = gps)
    private val declinationProvider = DeclinationFactory().getDeclinationStrategy(
        userPrefs,
        gps
    )
    private val isTrueNorth = userPrefs.compass.useTrueNorth
    private val formatter = FormatService.getInstance(context)
    
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

    var showReticle: Boolean = true
    var showPosition: Boolean = true

    /**
     * The diameter of the reticle in pixels
     */
    val reticleDiameter: Float
        get() = dp(36f)

    private val layers = mutableListOf<ARLayer>()
    private val layerLock = Any()

    // Guidance
    private var guideStrategy: ARPoint? = null
    private var guideThreshold: Float? = null
    private var onGuideReached: (() -> Unit)? = null

    fun start(useGPS: Boolean = true) {
        if (useGPS) {
            gps.start(this::onSensorUpdate)
            altimeter.start(this::onSensorUpdate)
        }
        // Recreate the orientation sensor - seems to be an upstream bug with the rotation vector that if you reuse, it may not be accurate
        orientationSensor.stop(this::onSensorUpdate)
        orientationSensor = sensors.getOrientation()
        orientationSensor.start(this::onSensorUpdate)
    }

    fun stop() {
        gps.stop(this::onSensorUpdate)
        altimeter.stop(this::onSensorUpdate)
        orientationSensor.stop(this::onSensorUpdate)
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
        guideStrategy: ARPoint,
        thresholdDegrees: Float? = null,
        onReached: () -> Unit = { clearGuide() }
    ) {
        this.guideStrategy = guideStrategy
        guideThreshold = thresholdDegrees
        onGuideReached = onReached
    }

    fun clearGuide() {
        guideStrategy = null
        guideThreshold = null
        onGuideReached = null
    }

    private fun onSensorUpdate(): Boolean {
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

        if (showReticle) {
            drawReticle()
            drawGuidance()
            drawFocusText()
        }

        if (showPosition) {
            drawPosition()
        }
    }

    private fun drawPosition() {
        val bearing = Bearing(azimuth)
        val azimuthText = formatter.formatDegrees(bearing.value, replace360 = true).padStart(4, ' ')
        val directionText = formatter.formatDirection(bearing.direction).padStart(2, ' ')
        val altitudeText = formatter.formatDegrees(inclination, replace360 = true)

        @SuppressLint("SetTextI18n")
        val text = "$azimuthText   $directionText\n${altitudeText}"
        drawText(text, width / 2f, drawer.dp(8f), drawer.sp(16f))
    }

    private fun drawGuidance() {
        // Draw an arrow around the reticle that points to the desired location
        val coordinate = guideStrategy?.getHorizonCoordinate(this) ?: return
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

    private fun drawText(text: String?, x: Float, y: Float, size: Float) {
        if (text.isNullOrBlank()) return

        val padding = dp(8f)
        val lineSpacing = dp(4f)
        val radius = dp(4f)

        // Background
        noStroke()
        fill(Color.BLACK.withAlpha(127))
        val totalDimensions = textDimensions(text, lineSpacing)
        rect(
            x - totalDimensions.first / 2f - padding,
            y,
            totalDimensions.first + padding * 2,
            totalDimensions.second + padding * 2,
            radius
        )


        fill(Color.WHITE)
        textSize(size)
        textMode(TextMode.Corner)
        textAlign(TextAlign.Center)

        // X is centered, Y is the bottom of the text
        val firstLineHeight = textHeight(text.split("\n").first())

        text(
            text,
            x,
            y + padding + firstLineHeight,
            lineSpacing
        )
    }

    private fun drawFocusText() {
        val textToRender = focusText ?: return

        drawText(
            textToRender,
            width / 2f,
            height / 2f + reticleDiameter / 2f + dp(8f),
            drawer.sp(16f)
        )
    }

    private fun drawReticle() {
        stroke(Color.WHITE.withAlpha(127))
        strokeWeight(dp(2f))
        noFill()
        circle(width / 2f, height / 2f, reticleDiameter)
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

        return AugmentedRealityUtils.getPixel(
            bearing,
            coordinate.elevation,
            coordinate.distance,
            rotationMatrix,
            size,
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
        return toPixel(HorizonCoordinate(bearing, elevationAngle, distance, true))
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
        size = Size(width.toFloat(), height.toFloat())

        AugmentedRealityUtils.getOrientation(
            orientationSensor,
            rotationMatrix,
            orientation,
            if (isTrueNorth) declinationProvider.getDeclination() else null
        )

        azimuth = orientation[0]
        inclination = orientation[1]
        sideInclination = orientation[2]
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

    private var camera: CameraView? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        camera?.onTouchEvent(event)
        return true
    }

    fun bind(camera: CameraView){
        this.camera = camera
        // TODO: Listen for fov and size changes
    }

    data class HorizonCoordinate(
        val bearing: Float,
        val elevation: Float,
        val distance: Float,
        val isTrueNorth: Boolean = true
    )

}
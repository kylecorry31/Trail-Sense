package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.Hysteresis
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.canvas.InteractionUtils
import com.kylecorry.trail_sense.shared.canvas.PixelCircle
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text
import com.kylecorry.trail_sense.shared.textDimensions
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.IARCalibrator
import com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper.CameraAnglePixelMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper.CameraAnglePixelMapperFactory
import com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper.SimplePerspectiveCameraAnglePixelMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARLayer
import kotlinx.coroutines.Dispatchers
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
        private set

    var focusText: String? = null
    private var hadFocus = false
    private var onFocusLostListener: (() -> Unit)? = null

    var backgroundFillColor: Int = Color.TRANSPARENT

    val rotationMatrix = FloatArray(16)
    private val orientation = FloatArray(3)
    private var previewRect: RectF? = null
    private var cameraMapper: CameraAnglePixelMapper? = null
    private val defaultMapper = SimplePerspectiveCameraAnglePixelMapper()

    // Sensors / preferences
    private val userPrefs = UserPreferences(context)
    private val sensors = SensorService(context)
    private var calibrationBearingOffset: Float = 0f
    val geomagneticOrientationSensor = sensors.getOrientation()
    val gyroOrientationSensor = sensors.getGyroscope()
    private var customOrientationSensor: IOrientationSensor? = null
    private val hasGyro = Sensors.hasGyroscope(context)
    var orientationSensor = geomagneticOrientationSensor
    val gps = sensors.getGPS(frequency = Duration.ofMillis(200))
    private val altimeter = sensors.getAltimeter(gps = gps)
    private val declinationProvider = DeclinationFactory().getDeclinationStrategy(
        userPrefs,
        gps
    )
    val isTrueNorth = userPrefs.compass.useTrueNorth
    private val formatter = FormatService.getInstance(context)

    private var fromTrueNorth = Quaternion.zero
    private var toTrueNorth = Quaternion.zero

    // Cache for strings
    private val hooks = Hooks()

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

    val locationAccuracy: Float?
        get() = gps.horizontalAccuracy

    /**
     * The altitude of the device in meters
     */
    val altitude: Float
        get() = altimeter.altitude

    var decimalPlaces = 0

    var passThroughTouchEvents = false

    var showReticle: Boolean = true
    var showPosition: Boolean = true

    var infiniteFocusWhenPointedUp: Boolean = false

    private var reticleColor = Color.WHITE.withAlpha(127)

    /**
     * The diameter of the reticle in pixels
     */
    var reticleDiameter: Float = 0f

    private val layers = mutableListOf<ARLayer>()
    private val layerLock = Any()

    // Guidance
    private var guidePoint: ARPoint? = null
    private var guideThreshold: Float? = null
    private var onGuideReached: (() -> Unit)? = null

    // Camera binding
    private var camera: CameraView? = null
    private val fovRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.Main)
    private var owner: LifecycleOwner? = null
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                syncTimer.interval(1000)
            }

            Lifecycle.Event.ON_PAUSE -> {
                syncTimer.stop()
                fovRunner.cancel()
            }

            else -> {} // Do nothing
        }
    }
    private val syncTimer = CoroutineTimer {
        syncWithCamera()
    }

    private val pointedUpTrigger = Hysteresis(30f, 5f)
    private var isSetup = false
    private val updateTimer = CoroutineTimer(observeOn = Dispatchers.Default) {
        if (!isSetup) {
            return@CoroutineTimer
        }
        updateOrientation()

        val isPointedUp = pointedUpTrigger.update(inclination)
        hooks.effect("focusAdjuster", camera, isPointedUp, infiniteFocusWhenPointedUp) {
            if (isPointedUp && infiniteFocusWhenPointedUp) {
                camera?.setFocus(1f)
            } else {
                camera?.setFocus(null)
            }
        }

        val layers = synchronized(layerLock) { layers.toList() }
        layers.forEach {
            it.update(this, this)
        }
    }

    fun start(useGPS: Boolean = true, customOrientationSensor: IOrientationSensor? = null) {
        this.customOrientationSensor = customOrientationSensor
        if (useGPS) {
            gps.start(this::onSensorUpdate)
            altimeter.start(this::onSensorUpdate)
        }
        orientationSensor = customOrientationSensor ?: geomagneticOrientationSensor
        calibrationBearingOffset = 0f
        geomagneticOrientationSensor.start(this::onSensorUpdate)
        customOrientationSensor?.start(this::onSensorUpdate)
        if (hasGyro) {
            gyroOrientationSensor.start(this::onSensorUpdate)
        }
        updateTimer.interval(20)
    }

    fun stop() {
        gps.stop(this::onSensorUpdate)
        altimeter.stop(this::onSensorUpdate)
        geomagneticOrientationSensor.stop(this::onSensorUpdate)
        gyroOrientationSensor.stop(this::onSensorUpdate)
        customOrientationSensor?.stop(this::onSensorUpdate)
        updateTimer.stop()
    }

    fun setLayers(layers: List<ARLayer>) {
        synchronized(layerLock) {
            this.layers.clear()
            this.layers.addAll(layers)
        }
    }

    fun guideTo(
        guidePoint: ARPoint,
        thresholdDegrees: Float? = null,
        onReached: () -> Unit = { clearGuide() }
    ) {
        this.guidePoint = guidePoint
        guideThreshold = thresholdDegrees
        onGuideReached = onReached
    }

    fun clearGuide() {
        guidePoint = null
        guideThreshold = null
        onGuideReached = null
    }

    fun setOnFocusLostListener(listener: (() -> Unit)?) {
        onFocusLostListener = listener
    }

    private fun onSensorUpdate(): Boolean {
        return true
    }


    override fun setup() {
        if (reticleDiameter == 0f) {
            reticleDiameter = dp(InteractionUtils.CLICK_SIZE_DP * 2f)
        }
        isSetup = true
        updateOrientation()
    }

    override fun draw() {
        background(backgroundFillColor)
        val layers = synchronized(layerLock) { layers.toList() }
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
            if (hadFocus) {
                onFocusLostListener?.invoke()
            }
            focusText = null
        }

        hadFocus = hasFocus

        if (showReticle) {
            drawGuidance()
            drawReticle()
            drawFocusText()
        }

        if (showPosition) {
            drawPosition()
        }
    }

    private fun drawPosition() {
        val bearing = Bearing(azimuth)
        val azimuthText = hooks.memo("azimuth_text", bearing.value.safeRoundPlaces(decimalPlaces)) {
            formatter.formatDegrees(bearing.value, decimalPlaces = decimalPlaces, replace360 = true)
                .padStart(4 + if (decimalPlaces == 0) 0 else (decimalPlaces + 1), ' ')
        }
        val directionText = hooks.memo("direction_text", bearing.direction) {
            formatter.formatDirection(bearing.direction).padStart(2, ' ')
        }
        val altitudeText =
            hooks.memo("altitude_text", inclination.safeRoundPlaces(decimalPlaces)) {
                formatter.formatDegrees(inclination, decimalPlaces)
            }

        @SuppressLint("SetTextI18n")
        val text = "$azimuthText   $directionText\n${altitudeText}"
        drawText(text, width / 2f, drawer.dp(8f), drawer.sp(16f))
    }

    private fun drawGuidance() {
        // Draw an arrow around the reticle that points to the desired location
        reticleColor = Color.WHITE.withAlpha(127)
        val coordinate = guidePoint?.getAugmentedRealityCoordinate(this) ?: return
        val threshold = guideThreshold
        val point = toPixel(coordinate)
        val center = PixelCoordinate(width / 2f, height / 2f)
        val reticle = PixelCircle(center, reticleDiameter / 2f)
        val circle = PixelCircle(
            point,
            if (threshold == null) reticleDiameter / 2f else sizeToPixel(threshold)
        )

        if (circle.contains(center)) {
            reticleColor = Color.WHITE
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
        stroke(reticleColor)
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

    /**
     * Gets the pixel coordinate of a point in the East-North-Up coordinate system
     * @param coordinate The augmented reality coordinate of the point
     * @return The pixel coordinate of the point
     */
    fun toPixel(coordinate: AugmentedRealityCoordinate): PixelCoordinate {
        val actual = getActualPoint(coordinate.position, coordinate.isTrueNorth)
        val screenPixel = AugmentedRealityUtils.getPixel(
            actual,
            rotationMatrix,
            previewRect ?: RectF(0f, 0f, width.toFloat(), height.toFloat()),
            fov,
            if (camera?.isStarted == true) cameraMapper ?: defaultMapper else defaultMapper
        )
        return PixelCoordinate(screenPixel.x - x, screenPixel.y - y)
    }

    fun toCoordinate(
        pixel: PixelCoordinate,
        rotationMatrixOverride: FloatArray? = null
    ): AugmentedRealityCoordinate {
        val screenPixel = PixelCoordinate(pixel.x + x, pixel.y + y)
        val rect = previewRect ?: RectF(0f, 0f, width.toFloat(), height.toFloat())
        val coordinate = AugmentedRealityUtils.getCoordinate(
            screenPixel,
            rotationMatrixOverride ?: rotationMatrix,
            rect,
            fov,
            if (camera?.isStarted == true) cameraMapper ?: defaultMapper else defaultMapper
        )

        return AugmentedRealityCoordinate(coordinate, isTrueNorth)
    }

    fun getActualPoint(point: Vector3, isPointTrueNorth: Boolean): Vector3 {
        return if (isTrueNorth && !isPointTrueNorth) {
            toTrueNorth.rotate(point)
        } else if (!isTrueNorth && isPointTrueNorth) {
            fromTrueNorth.rotate(point)
        } else {
            point
        }
    }

    private fun updateOrientation() {
        AugmentedRealityUtils.getOrientation(
            orientationSensor,
            rotationMatrix,
            orientation,
            (if (isTrueNorth) declinationProvider.getDeclination() else 0f) + calibrationBearingOffset
        )

        azimuth = orientation[0]
        inclination = orientation[1]
        sideInclination = orientation[2]

        // Update declination quaternions
        fromTrueNorth = Quaternion.from(Euler(0f, 0f, declinationProvider.getDeclination()))
        toTrueNorth = Quaternion.from(Euler(0f, 0f, -declinationProvider.getDeclination()))
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
        if (!isVisible) {
            return false
        }
        mGestureDetector.onTouchEvent(event)
        camera?.dispatchTouchEvent(event)
        return !passThroughTouchEvents
    }

    fun unbind() {
        owner?.lifecycle?.removeObserver(lifecycleObserver)
        syncTimer.stop()
        fovRunner.cancel()
        cameraMapper = null
        camera = null
        owner = null
    }

    fun bind(
        camera: CameraView,
        lifecycleOwner: LifecycleOwner? = null,
        defaultLayoutParams: ViewGroup.LayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    ) {
        this.camera = camera
        owner = lifecycleOwner ?: this.findViewTreeLifecycleOwner() ?: return

        if (layoutParams == null) {
            layoutParams = defaultLayoutParams
        }

        // Cancel fovRunner on pause
        owner?.lifecycle?.addObserver(lifecycleObserver)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        previewRect = null
        syncWithCamera()
    }

    fun switchToGyro() {
        val lastAzimuth = azimuth
        val lastInclination = inclination
        orientationSensor = if (hasGyro) gyroOrientationSensor else geomagneticOrientationSensor
        updateOrientation()
        calibrationBearingOffset = deltaAngle(azimuth, lastAzimuth)
    }

    suspend fun calibrate(calibrator: IARCalibrator, useGyro: Boolean = true) {
        val camera = camera ?: return
        calibrationBearingOffset = 0f
        // Switch to the reference orientation sensor and recalculate the orientation
        orientationSensor =
            if (useGyro && hasGyro) gyroOrientationSensor else geomagneticOrientationSensor
        updateOrientation()

        // Calculate the offset
        val offset = calibrator.calibrateBearing(this, camera) ?: return
        calibrationBearingOffset = offset
    }

    fun resetCalibration() {
        calibrationBearingOffset = 0f
        orientationSensor = geomagneticOrientationSensor
    }

    private fun syncWithCamera() {
        camera?.passThroughTouchEvents = true
        owner?.inBackground {
            fovRunner.enqueue {
                val camera = camera ?: return@enqueue
                if (!camera.isStarted) {
                    return@enqueue
                }

                val fov = camera.camera?.getPreviewFOV(false) ?: return@enqueue
                this@AugmentedRealityView.fov = Size(fov.first, fov.second)
                if (previewRect == null) {
                    previewRect = camera.camera?.getPreviewRect(false)
                }
                if (cameraMapper == null) {
                    cameraMapper = camera.camera?.let {
                        CameraAnglePixelMapperFactory().getMapper(
                            userPrefs.camera.projectionType,
                            it
                        )
                    }
                }
            }
        }
    }
}
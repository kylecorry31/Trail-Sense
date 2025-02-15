package com.kylecorry.trail_sense.shared.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.bitmaps.BitmapUtils.toBitmap
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.core.ui.setOnProgressChangeListener
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.R
import java.io.File
import java.time.Duration
import kotlin.math.roundToInt

class CameraView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    var camera: ICamera? = null

    private var lastFov: Pair<Float, Float>? = null

    val fov: Pair<Float, Float>
        get() {
            val defaultFOV = 45f
            val fieldOfView =
                camera?.getZoomedFOV() ?: lastFov ?: (defaultFOV to defaultFOV * 4f / 3f)
            lastFov = fieldOfView
            return fieldOfView
        }

    private val preview: PreviewView
    private val torchBtn: ImageButton
    private val changeCameraBtn: ImageButton
    private val zoomSeek: SeekBar
    private var zoomListener: ((Float) -> Unit)? = null
    private var imageListener: ((Bitmap) -> Unit)? = null
    private var captureListener: ((Bitmap) -> Unit)? = null
    private var isTorchOn = false
    private var zoom: Float = -1f
    private var isCapturing = false
    private var exposureCompensation = 0f
    private var exposureTime: Duration? = null
    private var sensitivity: Int? = null
    private var focus: Float? = null
    private var hasPendingChanges = false

    var passThroughTouchEvents = false

    val previewImage: Bitmap?
        get() = try {
            preview.bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    var isStarted = false
        private set
    private val startLock = Any()

    fun start(
        resolution: Size? = null,
        lifecycleOwner: LifecycleOwner? = null,
        captureSettings: ImageCaptureSettings? = null,
        readFrames: Boolean = true,
        shouldStabilizePreview: Boolean = true,
        preferBackCamera: Boolean = true,
        onImage: ((Bitmap) -> Unit)? = null
    ) {
        val owner = lifecycleOwner ?: this.findViewTreeLifecycleOwner() ?: return
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }

        if (!Camera.isAvailable(context)) {
            Alerts.toast(context, context.getString(R.string.camera_unavailable))
            return
        }

        val useBackCamera = preferBackCamera && Camera.hasBackCamera(context)

        if (!useBackCamera && !Camera.hasFrontCamera(context)) {
            Alerts.toast(context, context.getString(R.string.camera_unavailable))
            return
        }

        synchronized(startLock) {
            if (isStarted) {
                return
            }
            isStarted = true
        }

        // Keep the screen on while the camera is on
        keepScreenOn = true

        camera?.stop(this::onCameraUpdate)
        imageListener = onImage
        camera = Camera(
            context,
            owner,
            previewView = preview,
            analyze = readFrames,
            targetResolution = resolution,
            captureSettings = captureSettings,
            isBackCamera = useBackCamera,
            shouldStabilizePreview = shouldStabilizePreview
        )
        hasPendingChanges = true
        camera?.start(this::onCameraUpdate)
    }

    fun stop() {
        camera?.stop(this::onCameraUpdate)
        camera = null
        keepScreenOn = false
        imageListener = null
        synchronized(startLock) {
            isStarted = false
        }
    }

    suspend fun capture(file: File): Boolean {
        synchronized(this) {
            if (isCapturing) {
                return true
            }
            isCapturing = true
        }
        val success = camera?.takePhoto(file) ?: false
        synchronized(this) {
            isCapturing = false
        }
        return success
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setTorch(isOn: Boolean) {
        isTorchOn = isOn
        torchBtn.setImageResource(if (isOn) R.drawable.ic_torch_on else R.drawable.ic_torch_off)
        camera?.setTorch(isTorchOn)
        hasPendingChanges = true
    }

    fun setShowTorch(shouldShow: Boolean) {
        torchBtn.isVisible = shouldShow
    }

    fun setZoom(zoom: Float) {
        zoomSeek.progress = (zoom * 100).toInt()
        this.zoom = zoom
        val state = camera?.zoom
        val min = state?.ratioRange?.start ?: 1f
        val max = state?.ratioRange?.end ?: 2f
        camera?.setZoomRatio(SolMath.map(zoom, 0f, 1f, min, max))
        hasPendingChanges = true
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setZoomRatio(ratio: Float) {
        val state = camera?.zoom
        val min = state?.ratioRange?.start ?: 1f
        val max = state?.ratioRange?.end ?: 2f
        val pct = SolMath.norm(ratio, min, max)
        setZoom(pct)
    }

    fun setOnZoomChangeListener(listener: ((zoom: Float) -> Unit)?) {
        zoomListener = listener
    }

    fun setScaleType(type: PreviewView.ScaleType) {
        preview.scaleType = type
    }

    fun setPreviewBackgroundColor(@ColorInt color: Int) {
        preview.setBackgroundColor(color)
    }

    fun setExposureCompensation(value: Float) {
        exposureCompensation = value

        val range = camera?.getExposureCompensationRange() ?: Range(0, 0)
        val mapped = if (range.start == range.end) {
            range.start
        } else {
            SolMath.map(value, -1f, 1f, range.start.toFloat(), range.end.toFloat()).roundToInt()
        }

        camera?.setExposure(mapped)
        hasPendingChanges = true
    }

    fun setManualExposure(exposureTime: Duration?, sensitivity: Int?) {
        this.exposureTime = exposureTime
        this.sensitivity = sensitivity
        camera?.setExposureTime(exposureTime)
        camera?.setSensitivity(sensitivity)
        hasPendingChanges = true
    }

    fun setFocus(value: Float?) {
        focus = value
        camera?.setFocusDistancePercentage(value)
        hasPendingChanges = true
    }

    fun setPreviewColorFilter(filter: ColorFilter?) {
        if (filter == null) {
            preview.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            preview.setLayerType(LAYER_TYPE_NONE, null)
        } else {
            preview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            val paint = Paint()
            paint.colorFilter = filter
            preview.setLayerType(LAYER_TYPE_HARDWARE, paint)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        if (zoom == -1f) {
            setZoomRatio(1f)
        }
        if (hasPendingChanges) {
            println("Applying changes")
            setZoom(zoom)
            setExposureCompensation(exposureCompensation)
            setFocus(focus)
            setManualExposure(exposureTime, sensitivity)
            camera?.setTorch(isTorchOn)
            hasPendingChanges = false
        }
        if (captureListener == null && imageListener == null) {
            camera?.image?.close()
            return true
        }
        try {
            val rotation = camera?.image?.imageInfo?.rotationDegrees?.toFloat() ?: 0f
            val bitmap = camera?.image?.image?.toBitmap(rotation)
            bitmap?.let {
                captureListener?.invoke(it)
                imageListener?.invoke(it)

                synchronized(this) {
                    captureListener = null
                }
            }
        } catch (e: Exception) {
            // Do nothing
        } finally {
            camera?.image?.close()
        }
        return true
    }

    init {
        inflate(context, R.layout.view_camera, this)
        preview = findViewById(R.id.camera_preview)
        torchBtn = findViewById(R.id.camera_torch)
        zoomSeek = findViewById(R.id.camera_zoom)
        changeCameraBtn = findViewById(R.id.camera_change)

        val a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, 0, 0)

        torchBtn.setOnClickListener {
            setTorch(!isTorchOn)
        }

        if (a.getBoolean(R.styleable.CameraView_flipEnable, false)) {
            changeCameraBtn.visibility = View.VISIBLE
            changeCameraBtn.setOnClickListener {
                camera?.flipCamera()
            }
        }

        zoomSeek.setOnProgressChangeListener { progress, _ ->
            zoomListener?.invoke(progress / 100f)
            setZoom(progress / 100f)
        }

        a.recycle()
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoom != -1f) {
                val remainingZoom = 1 - zoom
                val newZoom = (zoom + remainingZoom / 2).coerceIn(0f, 1f)
                zoomListener?.invoke(newZoom)
                setZoom(newZoom)
                return true
            }
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return callOnClick() || super.onSingleTapConfirmed(e)
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (zoom != -1f) {
                val newZoom = (zoom - 1 + detector.scaleFactor).coerceIn(0f, 1f)
                zoomListener?.invoke(newZoom)
                setZoom(newZoom)
                return true
            }
            return false
        }
    }

    private val gestureDetector = GestureDetector(context, mGestureListener)
    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return !passThroughTouchEvents
    }


}
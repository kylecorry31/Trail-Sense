package com.kylecorry.trail_sense.shared.views.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.andromeda.camera.ImageCaptureSettings
import com.kylecorry.andromeda.camera.ZoomInfo
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath.toDegrees
import java.io.File
import java.io.OutputStream
import java.time.Duration
import java.util.concurrent.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.atan

class Camera(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val isBackCamera: Boolean = true,
    private val previewView: PreviewView? = null,
    private val analyze: Boolean = true,
    private val targetResolution: Size? = null,
    private val useYUV: Boolean = false,
    private val captureSettings: ImageCaptureSettings? = null,
    private val shouldStabilizePreview: Boolean = true,
    @AspectRatio.Ratio
    private val targetAspectRatio: Int = AspectRatio.RATIO_DEFAULT,
) : AbstractSensor(), ICamera {

    override val image: ImageProxy?
        get() = _image

    override val zoom: ZoomInfo?
        get() {
            try {
                val state = camera?.cameraInfo?.zoomState?.value ?: return null
                return ZoomInfo(
                    state.zoomRatio,
                    state.linearZoom,
                    Range(state.minZoomRatio, state.maxZoomRatio)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

    override val sensorRotation: Float
        get() {
            return try {
                camera?.cameraInfo?.sensorRotationDegrees?.toFloat() ?: 0f
            } catch (e: Exception) {
                e.printStackTrace()
                0f
            }
        }

    private var _image: ImageProxy? = null

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    private var _hasValidReading = false

    private var preview: Preview? = null

    override val hasValidReading: Boolean
        get() = _hasValidReading

    private var cachedFOV: Pair<Float, Float>? = null
    private var cachedFullPreviewSize: Size? = null
    private var cachedCroppedPreviewSize: Size? = null

    @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    override fun startImpl() {
        if (!Permissions.isCameraEnabled(context)) {
            return
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture?.addListener({
            try {
                cameraProvider = cameraProviderFuture?.get()
            } catch (e: CancellationException) {
                Log.i("Camera", "Unable to open camera because task was cancelled")
            } catch (e: InterruptedException) {
                Log.i("Camera", "Unable to open camera because task was interrupted")
            }
            preview = Preview.Builder()
                .also {
                    it.setTargetAspectRatio(targetAspectRatio)

                    // TODO: This might not be needed now that the method is exposed
                    if (!shouldStabilizePreview) {
                        tryOrLog {
                            // https://issuetracker.google.com/issues/230013960?pli=1
                            Camera2Interop.Extender(it)
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                                )
                                .setCaptureRequestOption(
                                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                                )
                        }
                    }
                }
                .build()

            val imageAnalysis = ImageAnalysis.Builder().apply {
                setTargetAspectRatio(targetAspectRatio)
                if (targetResolution != null) {
                    setTargetResolution(targetResolution)
                }
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                setOutputImageFormat(if (useYUV) OUTPUT_IMAGE_FORMAT_YUV_420_888 else OUTPUT_IMAGE_FORMAT_RGBA_8888)
            }.build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                _image = image
                _hasValidReading = true
                notifyListeners()
            }

            if (captureSettings != null) {
                val builder = ImageCapture.Builder()
                    .setFlashMode(captureSettings.flashMode)
                    .setCaptureMode(captureSettings.captureMode)

                captureSettings.quality?.let {
                    builder.setJpegQuality(it)
                }

                builder.setTargetAspectRatio(targetAspectRatio)

                captureSettings.rotation?.let {
                    builder.setTargetRotation(it)
                }

                targetResolution?.let {
                    builder.setTargetResolution(it)
                }

                imageCapture = builder.build()
            } else {
                imageCapture = null
            }

            val cameraSelector =
                if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            preview?.setSurfaceProvider(previewView?.surfaceProvider)

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *listOfNotNull(
                    if (previewView != null) preview else null,
                    if (analyze) imageAnalysis else null,
                    imageCapture
                ).toTypedArray()
            )

            notifyListeners()
        }, ContextCompat.getMainExecutor(context))

    }

    override fun stopImpl() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraProviderFuture?.cancel(true)
        cameraProviderFuture = null
        cachedFOV = null
    }

    override fun setLinearZoom(zoom: Float) {
        camera?.cameraControl?.setLinearZoom(zoom)
    }

    override fun setZoomRatio(zoom: Float) {
        camera?.cameraControl?.setZoomRatio(zoom)
    }

    override fun setExposure(index: Int) {
        tryOrLog {
            camera?.cameraControl?.setExposureCompensationIndex(index)
        }
    }

    override fun setTorch(isOn: Boolean) {
        camera?.cameraControl?.enableTorch(isOn)
        imageCapture?.flashMode = if (isOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    override fun stopFocusAndMetering() {
        tryOrNothing {
            camera?.cameraControl?.cancelFocusAndMetering()
        }
    }

    override fun startFocusAndMetering(point: PixelCoordinate) {
        val meteringPoint =
            previewView?.meteringPointFactory?.createPoint(point.x, point.y) ?: return
        val action = FocusMeteringAction.Builder(meteringPoint).build()

        tryOrNothing {
            camera?.cameraControl?.startFocusAndMetering(action)
        }
    }

    override fun takePhoto(callback: (image: ImageProxy?) -> Unit) {
        val imageCapture = imageCapture
        if (imageCapture == null) {
            callback(null)
            return
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                callback(image)
            }

            override fun onError(exception: ImageCaptureException) {
                callback(null)
            }
        })
    }

    override suspend fun takePhoto(): ImageProxy? = suspendCoroutine { cont ->
        takePhoto {
            cont.resume(it)
        }
    }

    override suspend fun takePhoto(file: File): Boolean = suspendCoroutine { cont ->
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        takePhoto(options, cont)
    }

    override suspend fun takePhoto(stream: OutputStream): Boolean = suspendCoroutine { cont ->
        val options = ImageCapture.OutputFileOptions.Builder(stream).build()
        takePhoto(options, cont)
    }

    private fun takePhoto(options: ImageCapture.OutputFileOptions, cont: Continuation<Boolean>) {
        val imageCapture = imageCapture
        if (imageCapture == null) {
            cont.resume(false)
            return
        }
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                cont.resume(true)
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resume(false)
            }
        })
    }

    @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    fun getCameraId(): String? {
        try {
            val info = getCamera2Info() ?: return getDefaultCameraId(context, isBackCamera)
            return info.cameraId
        } catch (e: Exception) {
            return getDefaultCameraId(context, isBackCamera)
        }
    }

    override fun getFOV(): Pair<Float, Float>? {
        cachedFOV?.let { return it }

        try {
            val activePixelSize =
                getCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    ?: return null
            val fullPixelSize =
                getCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                    ?: return null
            val maxFocus =
                getCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?: return null
            val size =
                getCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
            val sensorOrientation =
                getCharacteristic(CameraCharacteristics.SENSOR_ORIENTATION) ?: return null

            val w = size.width * activePixelSize.width() / fullPixelSize.width.toFloat()
            val h = size.height * activePixelSize.height() / fullPixelSize.height.toFloat()
            // TODO: The approach outlined in this stackoverflow post also handles cropping
            // https://stackoverflow.com/questions/39965408/what-is-the-android-camera2-api-equivalent-of-camera-parameters-gethorizontalvie
            val horizontalFOV = 2 * atan(w / (2 * maxFocus[0]))
            val verticalFOV = 2 * atan(h / (2 * maxFocus[0]))

            val isFlipped = sensorOrientation == 90 || sensorOrientation == 270

            val hFOV = if (isFlipped) {
                verticalFOV
            } else {
                horizontalFOV
            }

            val vFOV = if (isFlipped) {
                horizontalFOV
            } else {
                verticalFOV
            }

            val fov = hFOV.toDegrees() to vFOV.toDegrees()
            cachedFOV = fov
            return fov
        } catch (e: Exception) {
            return null
        }
    }

    override fun getZoomedFOV(): Pair<Float, Float>? {
        val fov = getFOV() ?: return null
        val zoom = zoom?.ratio?.coerceAtLeast(0.05f) ?: return fov
        return fov.first / zoom to fov.second / zoom
    }

    override fun setFocusDistancePercentage(distance: Float?) {
        if (distance == null) {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            return
        }

        val min = getCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val max = getCharacteristic(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE) ?: 0f
        val scaled = min + distance * (max - min)
        // Disable auto focus
        setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, scaled)
    }

    override fun setStabilization(opticalStabilization: Boolean, videoStabilization: Boolean) {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            if (videoStabilization) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        )

        setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            if (opticalStabilization) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
        )
    }

    override fun setExposureTime(exposureTime: Duration?) {
        if (exposureTime == null) {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
            return
        }

        val nanos = exposureTime.toNanos()
        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, nanos)
    }

    override fun setSensitivity(isoSensitivity: Int?) {
        if (isoSensitivity == null) {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
            return
        }

        setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
        setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoSensitivity)
    }

    override fun getExposureTimeRange(): Range<Duration>? {
        val range =
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) ?: return null
        return Range(Duration.ofNanos(range.lower), Duration.ofNanos(range.upper))
    }

    override fun getSensitivityRange(): Range<Int>? {
        val range =
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) ?: return null
        return Range(range.lower, range.upper)
    }

    override fun getExposureCompensationRange(): Range<Int>? {
        val range =
            getCharacteristic(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return null
        return Range(range.lower, range.upper)
    }

    override fun isOpticalStabilizationSupported(): Boolean {
        return getCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.any {
            it != CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
        } == true
    }

    override fun isVideoStabilizationSupported(): Boolean {
        return getCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.any {
            it != CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        } == true
    }

    override fun getPreviewRect(cropToView: Boolean): RectF? {
        return tryOrDefault(null) {
            val size = getPreviewSize(cropToView) ?: return@tryOrDefault null
            val previewX = previewView?.x ?: 0f
            val previewY = previewView?.y ?: 0f
            val left = when (previewView?.scaleType) {
                PreviewView.ScaleType.FILL_START, PreviewView.ScaleType.FIT_START -> 0
                PreviewView.ScaleType.FILL_CENTER, PreviewView.ScaleType.FIT_CENTER -> {
                    (previewView.width - size.width) / 2
                }

                PreviewView.ScaleType.FILL_END, PreviewView.ScaleType.FIT_END -> {
                    previewView.width - size.width
                }

                else -> 0
            }
            val top = when (previewView?.scaleType) {
                PreviewView.ScaleType.FILL_START, PreviewView.ScaleType.FIT_START -> 0
                PreviewView.ScaleType.FILL_CENTER, PreviewView.ScaleType.FIT_CENTER -> {
                    (previewView.height - size.height) / 2
                }

                PreviewView.ScaleType.FILL_END, PreviewView.ScaleType.FIT_END -> {
                    previewView.height - size.height
                }

                else -> 0
            }
            RectF(
                previewX + left,
                previewY + top,
                previewX + left + size.width,
                previewY + top + size.height
            )
        }
    }

    override fun getPreviewSize(cropToView: Boolean): Size? {
        // TODO: When should this be reset?
        if (cropToView && cachedCroppedPreviewSize != null){
            return cachedCroppedPreviewSize
        } else if (!cropToView && cachedFullPreviewSize != null){
            return cachedFullPreviewSize
        }

        val size = tryOrDefault(null) {
            val resolution = preview?.resolutionInfo?.resolution ?: return@tryOrDefault null
            val rotation = preview?.resolutionInfo?.rotationDegrees ?: return@tryOrDefault null
            val rotated = if (rotation == 90 || rotation == 270) {
                Size(resolution.height, resolution.width)
            } else {
                resolution
            }
            val viewSize =
                previewView?.let { Size(it.width, it.height) } ?: return@tryOrDefault null

            when (previewView.scaleType) {
                PreviewView.ScaleType.FILL_START, PreviewView.ScaleType.FILL_CENTER, PreviewView.ScaleType.FILL_END -> {
                    if (cropToView) {
                        viewSize
                    } else {
                        // Calculate the size of the preview after it's been scaled to fill the preview
                        val ratioBitmap = rotated.width.toFloat() / rotated.height.toFloat()
                        val ratioMax = viewSize.width.toFloat() / viewSize.height.toFloat()
                        var finalWidth = viewSize.width
                        var finalHeight = viewSize.height
                        if (ratioMax > ratioBitmap) {
                            finalHeight = (viewSize.width.toFloat() / ratioBitmap).toInt()
                        } else {
                            finalWidth = (viewSize.height.toFloat() * ratioBitmap).toInt()
                        }
                        Size(finalWidth, finalHeight)
                    }
                }

                PreviewView.ScaleType.FIT_START, PreviewView.ScaleType.FIT_CENTER, PreviewView.ScaleType.FIT_END -> {
                    // If it is smaller than the view, return the view size
                    if (rotated.width <= viewSize.width && rotated.height <= viewSize.height) {
                        return@tryOrDefault viewSize
                    }

                    // TODO: Extract this to a method
                    // Otherwise it gets scaled to fit the view
                    val ratioBitmap = rotated.width.toFloat() / rotated.height.toFloat()
                    val ratioMax = viewSize.width.toFloat() / viewSize.height.toFloat()
                    var finalWidth = viewSize.width
                    var finalHeight = viewSize.height
                    if (ratioMax > ratioBitmap) {
                        finalWidth = (viewSize.height.toFloat() * ratioBitmap).toInt()
                    } else {
                        finalHeight = (viewSize.width.toFloat() / ratioBitmap).toInt()
                    }
                    Size(finalWidth, finalHeight)
                }
            }
        }

        if (cropToView){
            cachedCroppedPreviewSize = size
        } else {
            cachedFullPreviewSize = size
        }

        return size
    }

    override fun getPreviewFOV(cropToView: Boolean): Pair<Float, Float>? {
        val fov = getZoomedFOV() ?: return null

        if (!cropToView) {
            return fov
        }

        val fullPreviewSize = getPreviewSize(false) ?: return null
        val croppedPreviewSize = getPreviewSize(true) ?: return null
        val xFov = if (fullPreviewSize.width > croppedPreviewSize.width) {
            fov.first * (croppedPreviewSize.width / fullPreviewSize.width.toFloat())
        } else {
            fov.first
        }
        val yFov = if (fullPreviewSize.height > croppedPreviewSize.height) {
            fov.second * (croppedPreviewSize.height / fullPreviewSize.height.toFloat())
        } else {
            fov.second
        }
        return Pair(xFov, yFov)
    }

    fun getCalibration(): FloatArray? {
        return getCharacteristic(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
    }

    fun getActiveArraySize(preCorrection: Boolean): Rect? {
        return if (preCorrection) {
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)
        } else {
            getCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        }
    }

    fun getDistortionCorrection(): FloatArray? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getCharacteristic(CameraCharacteristics.LENS_DISTORTION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            getCharacteristic(CameraCharacteristics.LENS_RADIAL_DISTORTION)
        } else {
            null
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCamera2Controller(): Camera2CameraControl? {
        val control = camera?.cameraControl ?: return null
        return Camera2CameraControl.from(control)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCamera2Info(): Camera2CameraInfo? {
        val info = camera?.cameraInfo ?: return null
        return Camera2CameraInfo.from(info)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun <T> getCharacteristic(key: CameraCharacteristics.Key<T>): T? {
        val info = getCamera2Info() ?: return null
        return info.getCameraCharacteristic(key)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun <T : Any> setCaptureRequestOption(key: CaptureRequest.Key<T>, value: T) {
        tryOrDefault(false) {
            val control = getCamera2Controller() ?: return
            val newCaptureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(key, value)
                .build()
            control.addCaptureRequestOptions(newCaptureRequestOptions)
        }
    }

    companion object {
        @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
        fun isAvailable(context: Context): Boolean {
            if (!Permissions.isCameraEnabled(context)) {
                return false
            }
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }

        fun hasBackCamera(context: Context): Boolean {
            val cameras = getCameras(context)
            return cameras.any {
                it.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
            }
        }

        fun hasFrontCamera(context: Context): Boolean {
            val cameras = getCameras(context)
            return cameras.any {
                it.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT
            }
        }

        private fun getDefaultCameraId(context: Context, isBackCamera: Boolean): String? {
            return tryOrDefault(null) {
                val manager =
                    context.getSystemService<CameraManager>() ?: return@tryOrDefault null
                manager.cameraIdList.firstOrNull {
                    val characteristics = manager.getCameraCharacteristics(it)
                    val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)
                    orientation == if (isBackCamera) CameraMetadata.LENS_FACING_BACK else CameraMetadata.LENS_FACING_FRONT
                }
            }
        }

        private fun getCameras(context: Context): List<CameraCharacteristics> {
            return tryOrDefault(emptyList()) {
                val manager =
                    context.getSystemService<CameraManager>() ?: return@tryOrDefault emptyList()
                manager.cameraIdList.map { manager.getCameraCharacteristics(it) }
            }
        }
    }

}
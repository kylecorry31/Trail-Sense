package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import androidx.annotation.ColorInt
import androidx.core.graphics.set
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.shared.AppColor
import kotlinx.coroutines.*

class CloudCoverageSensor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : AbstractSensor() {
    val coverage: Float
        get() = _coverage
    val luminance: Float
        get() = _luminance
    val dark: Float
        get() = _darkClouds
    val clouds: Bitmap?
        get() {
            return synchronized(this) {
                _clouds
            }
        }

    var bitmask: Boolean = false
    var skyDetectionSensitivity: Int = 50
    var obstacleRemovalSensitivity: Int = 0
    var zoom: Float = 1f
        set(value) {
            camera.setZoom(field - 1f)
            field = value
        }

    private val camera by lazy {
        Camera(
            context,
            lifecycleOwner,
            targetResolution = Size(100, 100),
            analyze = true
        )
    }

    private val cloudColorOverlay = Color.WHITE
    private val excludedColorOverlay = AppColor.Red.color
    private val skyColorOverlay = AppColor.Blue.color

    private var isRunning = false
    private val analysisLock = Object()

    private var job = Job()
    private var scope = CoroutineScope(Dispatchers.Default + job)

    private var _clouds: Bitmap? = null
    private var _coverage: Float = 0f
    private var _darkClouds: Float = 0f
    private var _luminance: Float = 0f

    override val hasValidReading: Boolean
        get() = true

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun startImpl() {
        if (!Camera.isAvailable(context)) {
            return
        }

        synchronized(analysisLock) {
            isRunning = false
        }
        job = Job()
        scope = CoroutineScope(Dispatchers.Default + job)
        camera.start(this::onCameraUpdate)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        val image = camera.image ?: return true
        synchronized(analysisLock) {
            if (!isRunning) {
                isRunning = true
                val bitmap = try {
                    image.image?.toBitmap()
                } catch (e: Exception) {
                    null
                }
                if (bitmap != null) {
                    scope.launch {
                        analyzeImage(bitmap)
                        synchronized(analysisLock) {
                            isRunning = false
                        }
                    }
                } else {
                    isRunning = false
                }
            }
            image.close()
        }
        return true
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private suspend fun analyzeImage(bitmap: Bitmap) {
        synchronized(this) {
            if (_clouds == null) {
                _clouds = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            }
        }

        var bluePixels = 0
        var cloudPixels = 0
        var cloudColor = 0.0
        var darkCloudPixels = 0

        val isSky = BGIsSkySpecification(100 - skyDetectionSensitivity)

        val isObstacle = SaturationIsObstacleSpecification(1 - obstacleRemovalSensitivity / 100f)

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                if (isSky.isSatisfiedBy(pixel)) {
                    bluePixels++
                    if (bitmask) {
                        setCloudPixel(w, h, skyColorOverlay)
                    } else {
                        setCloudPixel(w, h, pixel)
                    }
                } else if (isObstacle.isSatisfiedBy(pixel)) {
                    if (bitmask) {
                        setCloudPixel(w, h, excludedColorOverlay)
                    } else {
                        setCloudPixel(w, h, pixel)
                    }
                } else {
                    cloudPixels++
                    val l = luminance(pixel)
                    cloudColor += l
                    if (l < 0.33){
                        darkCloudPixels++
                    }
                    if (bitmask) {
                        setCloudPixel(w, h, cloudColorOverlay)
                    } else {
                        setCloudPixel(w, h, pixel)
                    }
                }

            }
        }
        bitmap.recycle()

        _coverage = if (bluePixels + cloudPixels != 0) {
            cloudPixels / (bluePixels + cloudPixels).toFloat()
        } else {
            0f
        }

        _luminance = if (cloudPixels != 0) {
            (cloudColor / cloudPixels).toFloat()
        } else {
            0f
        }

        _darkClouds = if (cloudPixels != 0) {
            darkCloudPixels / cloudPixels.toFloat()
        } else {
            0f
        }

        withContext(Dispatchers.Main) {
            notifyListeners()
        }
    }

    private fun setCloudPixel(x: Int, y: Int, @ColorInt color: Int) {
        synchronized(this) {
            _clouds?.set(x, y, color)
        }
    }

    private fun luminance(@ColorInt color: Int): Float {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return (0.2126 * r + 0.7152 * g + 0.0722 * b).toFloat()
    }

    override fun stopImpl() {
        camera.stop(this::onCameraUpdate)
        if (scope.isActive) {
            tryOrNothing {
                scope.cancel()
            }
        }
        synchronized(analysisLock) {
            isRunning = false
        }
        synchronized(this) {
            _clouds?.recycle()
            _clouds = null
        }
    }
}
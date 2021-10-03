package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import androidx.core.graphics.set
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.science.meteorology.clouds.CloudType
import com.kylecorry.trail_sense.shared.AppColor
import kotlinx.coroutines.*

class CloudSensor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : AbstractSensor() {
    val cover: Float
        get() = _coverage
    val luminance: Float
        get() = _luminance
    val contrast: Float
        get() = _contrast
    val cloudType: CloudType?
        get() = _cloudType
    val cloudTypeConfidence: Float?
        get() = _cloudTypeConfidence
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

    private var job = Job()
    private var scope = CoroutineScope(Dispatchers.Default + job)

    private var _clouds: Bitmap? = null
    private var _coverage: Float = 0f
    private var _luminance: Float = 0f
    private var _contrast: Float = 0f
    private var _cloudType: CloudType? = null
    private var _cloudTypeConfidence: Float? = null
    private var override: Bitmap? = null

    override val hasValidReading: Boolean
        get() = true

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun startImpl() {
        if (!Camera.isAvailable(context)) {
            return
        }
        job = Job()
        scope = CoroutineScope(Dispatchers.Default + job)
//        override = Resources.drawable(context, R.drawable.nimbostratus)?.toBitmap()
        camera.start(this::onCameraUpdate)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun onCameraUpdate(): Boolean {
        val image = camera.image ?: return true

        scope.launch {
            val bitmap = try {
                override ?: image.image?.toBitmap()
            } catch (e: Exception) {
                null
            }
            bitmap?.let {
                analyzeImage(it)
            }
            image.close()
        }
        return true
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private suspend fun analyzeImage(bitmap: Bitmap) {
        synchronized(this) {
            if (_clouds == null) {
                _clouds = bitmap.copy(bitmap.config, true)
            } else if (!bitmask) {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                _clouds?.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            }
        }

        val analyzer = CloudAnalyzer(
            skyDetectionSensitivity,
            obstacleRemovalSensitivity,
            skyColorOverlay,
            excludedColorOverlay,
            cloudColorOverlay
        )

        val observation = withContext(Dispatchers.IO) {
            analyzer.getClouds(bitmap) { x, y, pixel ->
                if (bitmask) {
                    synchronized(this) {
                        _clouds?.set(x, y, pixel)
                    }
                }
            }
        }
        _coverage = observation.cover
        _luminance = observation.luminance
        _contrast = observation.contrast
        _cloudType = observation.type
        _cloudTypeConfidence = observation.typeConfidence

        if (bitmap != override) {
            bitmap.recycle()
        }

        withContext(Dispatchers.Main) {
            notifyListeners()
        }
    }

    override fun stopImpl() {
        camera.stop(this::onCameraUpdate)
        tryOrNothing {
            scope.cancel()
        }
    }

    fun destroy() {
        synchronized(this) {
            _clouds?.recycle()
            _clouds = null
        }
    }
}
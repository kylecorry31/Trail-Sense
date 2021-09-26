package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import androidx.annotation.ColorInt
import androidx.camera.core.ImageProxy
import androidx.core.graphics.set
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.trail_sense.shared.AppColor
import com.kylecorry.trail_sense.shared.specifications.FalseSpecification

class CloudCoverageSensor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : AbstractSensor() {

    private val camera by lazy {
        Camera(
            context,
            lifecycleOwner,
            targetResolution = Size(200, 200),
            analyze = true
        )
    }

    private val cloudColorOverlay = AppColor.Green.color
    private val excludedColorOverlay = AppColor.Red.color

    val coverage: Float
        get() = _coverage
    val clouds: Bitmap?
        get() {
            return synchronized(this) {
                _clouds
            }
        }

    var bitmask: Boolean = false
    var skyThreshold: Int = 30
    var excludeObstacles = false
    var excludeSun = false

    private var _clouds: Bitmap? = null
    private var _coverage: Float = 0f

    override val hasValidReading: Boolean
        get() = true

    fun setZoom(zoom: Float) {
        camera.setZoom(zoom)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun startImpl() {
        if (!Camera.isAvailable(context)) {
            return
        }

        camera.start(this::onCameraUpdate)
    }

    private fun onCameraUpdate(): Boolean {
        val image = camera.image ?: return true
        analyzeImage(image)
        return true
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun analyzeImage(image: ImageProxy) {
        val bitmap = try {
            image.image?.toBitmap() ?: return
        } catch (e: Exception) {
            return
        }
        synchronized(this) {
            if (_clouds == null) {
                _clouds = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            }
        }

        var bluePixels = 0
        var cloudPixels = 0

        val isSky = BGIsSkySpecification(skyThreshold)

        val isObstacle = if (excludeObstacles) {
            ColorVarianceIsObstacleSpecification(20).or(LuminanceIsObstacleSpecification(50))
        } else {
            FalseSpecification()
        }

        val isSun = if (excludeSun) {
            IsSunSpecification()
        } else {
            FalseSpecification()
        }

        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)

                if (isSky.isSatisfiedBy(pixel) || isSun.isSatisfiedBy(pixel)) {
                    bluePixels++
                    if (bitmask) {
                        _clouds?.set(w, h, Color.BLACK)
                    } else {
                        _clouds?.set(w, h, pixel)
                    }
                } else if (isObstacle.isSatisfiedBy(pixel)) {
                    if (bitmask) {
                        _clouds?.set(w, h, Color.BLACK)
                    } else {
                        _clouds?.set(w, h, addColors(pixel, excludedColorOverlay))
                    }
                } else {
                    cloudPixels++
                    if (bitmask) {
                        _clouds?.set(w, h, Color.WHITE)
                    } else {
                        _clouds?.set(w, h, addColors(pixel, cloudColorOverlay))
                    }
                }

            }
        }
        bitmap.recycle()
        image.close()

        _coverage = if (bluePixels + cloudPixels != 0) {
            cloudPixels / (bluePixels + cloudPixels).toFloat()
        } else {
            0f
        }

        notifyListeners()
    }

    @ColorInt
    private fun addColors(@ColorInt color1: Int, @ColorInt color2: Int): Int {
        return Color.argb(
            Color.alpha(color1),
            (Color.red(color1) + Color.red(color2)).coerceAtMost(255),
            (Color.green(color1) + Color.green(color2)).coerceAtMost(255),
            (Color.blue(color1) + Color.blue(color2)).coerceAtMost(255)
        )
    }

    override fun stopImpl() {
        camera.stop(this::onCameraUpdate)
        synchronized(this) {
            _clouds?.recycle()
            _clouds = null
        }
    }
}
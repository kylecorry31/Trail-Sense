package com.kylecorry.trail_sense.weather.infrastructure.clouds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.core.graphics.set
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.core.bitmap.BitmapUtils.toBitmap
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.trail_sense.shared.AppColor

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

    private var _clouds: Bitmap? = null
    private var _coverage: Float = 0f

    override val hasValidReading: Boolean
        get() = true

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
        val total = bitmap.width * bitmap.height.toFloat()
        for (w in 0 until bitmap.width) {
            for (h in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(w, h)
                val blue = Color.blue(pixel)
                val green = Color.green(pixel)

                val bg = blue - green

                val isSky = bg >= skyThreshold

                if (isSky) {
                    bluePixels++
                    if (bitmask) {
                        _clouds?.set(w, h, Color.BLACK)
                    } else {
                        _clouds?.set(w, h, pixel)
                    }
                } else {
                    if (bitmask) {
                        _clouds?.set(w, h, Color.WHITE)
                    } else {
                        _clouds?.set(
                            w,
                            h,
                            Color.argb(
                                255,
                                (Color.red(pixel) + Color.red(cloudColorOverlay)).coerceAtMost(255),
                                (green + Color.green(cloudColorOverlay)).coerceAtMost(255),
                                (blue + Color.blue(cloudColorOverlay)).coerceAtMost(255)
                            )
                        )
                    }
                }

            }
        }
        bitmap.recycle()
        image.close()

        _coverage = 1 - bluePixels / total

        notifyListeners()
    }

    override fun stopImpl() {
        camera.stop(this::onCameraUpdate)
        synchronized(this) {
            _clouds?.recycle()
            _clouds = null
        }
    }
}
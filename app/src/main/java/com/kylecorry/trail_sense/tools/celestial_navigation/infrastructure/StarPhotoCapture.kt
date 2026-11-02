package com.kylecorry.trail_sense.tools.celestial_navigation.infrastructure

import com.kylecorry.andromeda.camera.ICamera
import com.kylecorry.sol.science.optics.PinholeCamera
import com.kylecorry.trail_sense.tools.celestial_navigation.domain.LuminanceImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object StarPhotoCapture {

    class Frame(val image: LuminanceImage, val startMillis: Long, val endMillis: Long)

    /**
     * Takes one or more photos and sums their luminance, rotated to the device's portrait orientation
     */
    suspend fun capture(camera: ICamera, stack: Int = 1): Frame? {
        val startMillis = System.currentTimeMillis()
        var sum: FloatArray? = null
        var width = 0
        var height = 0
        for (i in 0 until stack.coerceAtLeast(1)) {
            val proxy = camera.takePhoto() ?: return null
            val rotation = proxy.imageInfo.rotationDegrees
            val raw = proxy.toBitmap()
            proxy.close()
            withContext(Dispatchers.Default) {
                // Rotate while reading so a second full size bitmap is never needed
                val rawWidth = raw.width
                val rawHeight = raw.height
                val swap = rotation == 90 || rotation == 270
                width = if (swap) rawHeight else rawWidth
                height = if (swap) rawWidth else rawHeight
                val data = sum ?: FloatArray(width * height).also { sum = it }
                val row = IntArray(rawWidth)
                for (y in 0 until rawHeight) {
                    raw.getPixels(row, 0, rawWidth, 0, y, rawWidth, 1)
                    for (x in 0 until rawWidth) {
                        val p = row[x]
                        val index = when (rotation) {
                            90 -> x * width + (rawHeight - 1 - y)
                            180 -> (rawHeight - 1 - y) * width + (rawWidth - 1 - x)
                            270 -> (rawWidth - 1 - x) * width + y
                            else -> y * width + x
                        }
                        data[index] += 0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
                    }
                }
                raw.recycle()
            }
        }
        return Frame(LuminanceImage(width, height, checkNotNull(sum)), startMillis, System.currentTimeMillis())
    }

    fun cameraModel(
        camera: ICamera?,
        width: Int,
        height: Int,
        useIntrinsics: Boolean = true,
        focalOverride: Double = 0.0
    ): PinholeCamera {
        if (focalOverride > 0) return PinholeCamera(width, height, focalOverride)
        if (useIntrinsics) {
            val intrinsics = camera?.getIntrinsicCalibration(estimateIfUnavailable = false)
            val active = camera?.getActiveArraySize(false)
            if (intrinsics != null && active != null && intrinsics[0] > 0) {
                val scale = max(width, height).toDouble() / max(active.width(), active.height())
                return PinholeCamera(width, height, intrinsics[0] * scale)
            }
        }
        val fov = camera?.getFOV()
        return if (fov != null) {
            PinholeCamera.fromVerticalFieldOfView(width, height, fov.second.toDouble())
        } else {
            PinholeCamera.fromVerticalFieldOfView(width, height, 66.0)
        }
    }
}

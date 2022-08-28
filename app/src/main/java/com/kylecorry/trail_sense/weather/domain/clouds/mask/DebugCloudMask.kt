package com.kylecorry.trail_sense.weather.domain.clouds.mask

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.colors.ColorUtils

class DebugCloudMask : ICloudMask {

    override fun mask(input: Bitmap, output: Bitmap?): Bitmap {
        val out = output ?: input.copy(input.config, true)
        for (x in 0 until input.width) {
            for (y in 0 until input.height) {
                val pixel = input.getPixel(x, y)
                val value = SolMath.map(ColorUtils.nrbr(pixel), -1f, 1f, 0f, 255f).toInt()
                out[x, y] = Color.rgb(value, value, value)
            }
        }
        return out
    }
}
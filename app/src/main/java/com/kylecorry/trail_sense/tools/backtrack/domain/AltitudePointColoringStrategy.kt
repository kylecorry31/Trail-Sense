package com.kylecorry.trail_sense.tools.backtrack.domain

import android.util.Range
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.kylecorry.andromeda.core.math.constrain
import com.kylecorry.andromeda.core.math.norm
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class AltitudePointColoringStrategy(
    private val altitudeRange: Range<Float>,
    @ColorInt private val lowestColor: Int,
    @ColorInt private val highestColor: Int
) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        val altitude = point.elevation ?: return lowestColor
        val ratio = constrain(norm(altitude, altitudeRange.lower, altitudeRange.upper), 0f, 1f)
        return ColorUtils.blendARGB(lowestColor, highestColor, ratio)
    }
}
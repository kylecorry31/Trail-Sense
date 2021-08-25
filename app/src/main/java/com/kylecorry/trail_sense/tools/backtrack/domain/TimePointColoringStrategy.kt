package com.kylecorry.trail_sense.tools.backtrack.domain

import android.util.Range
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.kylecorry.andromeda.core.math.constrain
import com.kylecorry.andromeda.core.math.norm
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import java.time.Instant

class TimePointColoringStrategy(
    private val timeRange: Range<Instant>,
    @ColorInt private val oldestColor: Int,
    @ColorInt private val newestColor: Int
) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int {
        val time = point.time ?: return oldestColor
        val ratio = constrain(
            norm(
                time.toEpochMilli().toFloat(),
                timeRange.lower.toEpochMilli().toFloat(),
                timeRange.upper.toEpochMilli().toFloat()
            ), 0f, 1f
        )
        return ColorUtils.blendARGB(oldestColor, newestColor, ratio)
    }
}
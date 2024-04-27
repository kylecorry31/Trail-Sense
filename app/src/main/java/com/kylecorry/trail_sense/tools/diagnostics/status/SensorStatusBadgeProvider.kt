package com.kylecorry.trail_sense.tools.diagnostics.status

import android.content.Context
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService

class SensorStatusBadgeProvider(
    private val sensor: ISensor,
    private val context: Context,
    @DrawableRes private val icon: Int
) : StatusBadgeProvider {

    private val formatter = FormatService.getInstance(context)
    private val isUnavailable = sensor is MockSensor

    override fun getBadge(): StatusBadge {
        if (isUnavailable) {
            return StatusBadge(
                context.getString(R.string.unavailable),
                CustomUiUtils.getQualityColor(Quality.Unknown),
                icon
            )
        }

        return StatusBadge(
            formatter.formatQuality(sensor.quality),
            CustomUiUtils.getQualityColor(sensor.quality),
            icon
        )
    }
}
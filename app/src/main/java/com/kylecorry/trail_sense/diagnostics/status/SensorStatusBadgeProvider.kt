package com.kylecorry.trail_sense.diagnostics.status

import android.content.Context
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService

class SensorStatusBadgeProvider(
    private val sensor: ISensor,
    context: Context,
    @DrawableRes private val icon: Int
) : StatusBadgeProvider {

    private val formatter = FormatService.getInstance(context)

    override fun getBadge(): StatusBadge {
        return StatusBadge(
            formatter.formatQuality(sensor.quality),
            CustomUiUtils.getQualityColor(sensor.quality),
            icon
        )
    }
}
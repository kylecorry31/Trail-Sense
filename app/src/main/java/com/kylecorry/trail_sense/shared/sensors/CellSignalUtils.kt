package com.kylecorry.trail_sense.shared.sensors

import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.R

object CellSignalUtils {

    @DrawableRes
    fun getCellQualityImage(quality: Quality?): Int {
        return when(quality){
            Quality.Poor -> R.drawable.signal_cellular_1
            Quality.Moderate -> R.drawable.signal_cellular_2
            Quality.Good -> R.drawable.signal_cellular_3
            else -> R.drawable.signal_cellular_outline
        }
    }

}
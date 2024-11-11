package com.kylecorry.trail_sense.tools.tides.ui

import android.content.Context
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.R

class TideFormatter(private val context: Context) {

    fun getTideTypeName(tideType: TideType?): String {
        return when (tideType) {
            TideType.High -> context.getString(R.string.high_tide)
            TideType.Low -> context.getString(R.string.low_tide)
            null -> context.getString(R.string.half_tide)
        }
    }

    fun getTideTypeImage(tideType: TideType?): Int {
        return when (tideType) {
            TideType.High -> R.drawable.ic_tide_high
            TideType.Low -> R.drawable.ic_tide_low
            null -> R.drawable.ic_tide_half
        }
    }
}
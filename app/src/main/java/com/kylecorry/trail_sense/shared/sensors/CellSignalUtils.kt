package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import androidx.annotation.DrawableRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.network.CellNetwork
import com.kylecorry.trailsensecore.domain.units.Quality

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

    fun getCellTypeString(context: Context, cellType: CellNetwork?): String {
        return when (cellType){
            CellNetwork.Nr -> context.getString(R.string.network_5g)
            CellNetwork.Lte -> context.getString(R.string.network_4g)
            CellNetwork.Cdma, CellNetwork.Gsm -> context.getString(R.string.network_2g)
            CellNetwork.Wcdma -> context.getString(R.string.network_3g)
            else -> context.getString(R.string.network_no_signal)
        }
    }

}
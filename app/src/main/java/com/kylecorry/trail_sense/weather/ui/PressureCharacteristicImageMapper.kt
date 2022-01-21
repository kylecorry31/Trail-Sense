package com.kylecorry.trail_sense.weather.ui

import androidx.annotation.DrawableRes
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.trail_sense.R

class PressureCharacteristicImageMapper {

    @DrawableRes
    fun getImageResource(characteristic: PressureCharacteristic): Int {
        return when (characteristic) {
            PressureCharacteristic.Falling, PressureCharacteristic.FallingFast -> R.drawable.ic_arrow_down
            PressureCharacteristic.Rising, PressureCharacteristic.RisingFast -> R.drawable.ic_arrow_up
            else -> R.drawable.ic_steady_arrow
        }
    }

}
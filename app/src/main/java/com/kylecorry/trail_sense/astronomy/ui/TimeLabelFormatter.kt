package com.kylecorry.trail_sense.astronomy.ui

import android.content.Context
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kylecorry.trail_sense.shared.toDisplayFormat
import java.time.LocalTime
import kotlin.math.roundToInt


class TimeLabelFormatter(private val context: Context) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        if (value % 4 == 0f){
            val localTime = LocalTime.of(value.roundToInt() % 24, 0)
            return localTime.toDisplayFormat(context, true)
        }

        return ""
    }
}
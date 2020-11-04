package com.kylecorry.trail_sense.shared

import java.text.DecimalFormat

object DecimalFormatter {
    fun format(number: Double): String {
        val fmt = DecimalFormat("#.####")
        return fmt.format(number)
    }
}
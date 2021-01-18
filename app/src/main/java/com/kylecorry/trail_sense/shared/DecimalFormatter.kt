package com.kylecorry.trail_sense.shared

import java.text.DecimalFormat

object DecimalFormatter {
    fun format(number: Double): String {
        val fmt = DecimalFormat("#.####")
        return fmt.format(number)
    }

    fun format(number: Float, decimalPlaces: Int): String {
        return format(number.toDouble(), decimalPlaces)
    }

    fun format(number: Double, decimalPlaces: Int): String {
        if (decimalPlaces <= 0){
            return DecimalFormat("#").format(number)
        }

        val builder = StringBuilder("#.")
        for (i in 0..decimalPlaces){
            builder.append('#')
        }

        val fmt = DecimalFormat(builder.toString())
        return fmt.format(number)
    }
}
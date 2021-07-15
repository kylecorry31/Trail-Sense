package com.kylecorry.trail_sense.shared

import android.graphics.Color
import androidx.annotation.ColorInt

enum class AppColor(val id: Int, @ColorInt val color: Int) {
    Red(0, Color.parseColor("#ef5350")),
    Orange(1, Color.parseColor("#FF6D00")),
    Yellow(2, Color.parseColor("#DDCE2C")),
    Green(3, Color.parseColor("#81c784")),
    Blue(4, Color.parseColor("#a0caff")),
    Purple(5, Color.parseColor("#c158dc")),
    Pink(6, Color.parseColor("#ff33dd"))
    // TODO: Have a text color primary / secondary app color
    // TODO: Have a black / white app color
}
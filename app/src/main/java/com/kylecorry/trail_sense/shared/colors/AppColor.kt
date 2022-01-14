package com.kylecorry.trail_sense.shared.colors

import android.graphics.Color
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.database.Identifiable

enum class AppColor(override val id: Long, @ColorInt override val color: Int): IAppColor {
    Red(0, Color.parseColor("#ef5350")),
    Orange(1, Color.parseColor("#FF6D00")),
    Yellow(2, Color.parseColor("#DDCE2C")),
    Green(3, Color.parseColor("#81c784")),
    Blue(4, Color.parseColor("#a0caff")),
    Purple(5, Color.parseColor("#c158dc")),
    Pink(6, Color.parseColor("#ff77a9")),
    Gray(7, Color.parseColor("#9e9e9e")),
    Brown(8, Color.parseColor("#a98274")),
    DarkBlue(9, Color.parseColor("#304ffe"))
}

interface IAppColor: Identifiable {
    val color: Int
}
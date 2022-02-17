package com.kylecorry.trail_sense.shared.colors

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.database.Identifiable

enum class AppColor(override val id: Long, @ColorInt override val color: Int): IAppColor {
    Red(0, -1092784), // #ef5350
    Orange(1, -37632), // #FF6D00
    Yellow(2, -2240980), // #DDCE2C
    Green(3, -8271996), // #81c784
    Blue(4, -6239489), // #a0caff
    Purple(5, -4106020), // #c158dc
    Pink(6, -34903), // #ff77a9
    Gray(7, -6381922), // #9e9e9e
    Brown(8, -5668236), // #a98274
    DarkBlue(9, -13611010) // #304ffe
}

fun Array<AppColor>.fromColor(@ColorInt color: Int): AppColor? {
    return firstOrNull { it.color == color }
}

interface IAppColor: Identifiable {
    val color: Int
}
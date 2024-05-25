package com.kylecorry.trail_sense.shared.colors

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.R

enum class AppColor(override val id: Long, @ColorInt override val color: Int): IAppColor {
    Red(0, R.color.red), // #ef5350
    Orange(1, R.color.orange), // #FF6D00
    Yellow(2, R.color.yellow), // #DDCE2C
    Green(3, R.color.green), // #81c784
    Blue(4, R.color.blue), // #a0caff
    Purple(5, R.color.purple), // #c158dc
    Pink(6, R.color.pink), // #ff77a9
    Gray(7, R.color.gray), // #9e9e9e
    Brown(8, R.color.brown), // #a98274
    DarkBlue(9, R.color.dark_blue) // #304ffe
}

fun Array<AppColor>.fromColor(@ColorInt color: Int): AppColor? {
    return firstOrNull { it.color == color }
}

interface IAppColor: Identifiable {
    val color: Int
}
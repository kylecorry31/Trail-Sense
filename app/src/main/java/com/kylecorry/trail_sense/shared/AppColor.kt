package com.kylecorry.trail_sense.shared

import androidx.annotation.ColorRes
import com.kylecorry.trail_sense.R

enum class AppColor(val id: Int, @ColorRes val color: Int) {
    Red(0, R.color.red),
    Orange(1, R.color.orange),
    Yellow(2, R.color.yellow),
    Green(3, R.color.green),
    Blue(4, R.color.blue),
    Purple(5, R.color.purple),
    Pink(6, R.color.pink)
}
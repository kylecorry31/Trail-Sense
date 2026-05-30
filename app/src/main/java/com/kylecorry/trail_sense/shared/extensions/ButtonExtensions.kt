package com.kylecorry.trail_sense.shared.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

fun MaterialButton.flatten() {
    backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
    iconTint = ColorStateList.valueOf(
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
    )
    strokeWidth = 0
    elevation = 0f
}

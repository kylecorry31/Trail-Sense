package com.kylecorry.trail_sense.shared.extensions

import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible

fun SwitchCompat.bindVisibility(vararg views: View) {
    views.forEach { it.isVisible = isChecked }
    setOnCheckedChangeListener { _, isChecked ->
        views.forEach { it.isVisible = isChecked }
    }
}
package com.kylecorry.trail_sense.shared.extensions

import android.widget.EditText
import com.kylecorry.andromeda.core.toFloatCompat

fun EditText.floatValue(): Float? {
    return text?.toString()?.toFloatCompat()
}
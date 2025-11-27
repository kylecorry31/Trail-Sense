package com.kylecorry.trail_sense.shared.text

import android.content.Context
import androidx.annotation.StringRes

class StringLoader(private val context: Context) {
    fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }

    fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
        return context.getString(id, *formatArgs)
    }
}
package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import androidx.annotation.StringRes
import com.kylecorry.andromeda.preferences.Preferences

abstract class PreferenceRepo(protected val context: Context) {

    protected val cache by lazy { Preferences(context) }

    protected fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }
}
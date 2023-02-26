package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

abstract class PreferenceRepo(protected val context: Context) {

    protected val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    protected fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }
}
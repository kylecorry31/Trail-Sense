package com.kylecorry.trail_sense.settings

import android.content.Context
import androidx.annotation.StringRes
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache

abstract class PreferenceRepo(protected val context: Context) {

    protected val cache by lazy { Cache(context) }

    protected fun getString(@StringRes id: Int): String {
        return context.getString(id)
    }
}
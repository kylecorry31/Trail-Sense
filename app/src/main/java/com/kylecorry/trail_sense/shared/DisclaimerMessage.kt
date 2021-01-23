package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class DisclaimerMessage(private val context: Context) {

    private val cache by lazy { Cache(context) }

    fun shouldShow(): Boolean {
        return cache.getBoolean(PREF_KEY) ?: true
    }

    fun show() {
        cache.putBoolean(PREF_KEY, false)
        UiUtils.alert(
            context, context.getString(R.string.disclaimer_message_title), context.getString(
                R.string.disclaimer_message_content
            ), R.string.dialog_ok
        )
    }


    companion object {
        private const val PREF_KEY = "pref_show_disclaimer_message"
    }

}
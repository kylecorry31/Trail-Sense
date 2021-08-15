package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R

class DisclaimerMessage(private val context: Context) {

    private val cache by lazy { Preferences(context) }

    fun shouldShow(): Boolean {
        return cache.getBoolean(PREF_KEY) ?: true
    }

    fun show() {
        cache.putBoolean(PREF_KEY, false)
        Alerts.dialog(
            context, context.getString(R.string.app_disclaimer_message_title), context.getString(
                R.string.disclaimer_message_content
            ), cancelText = null
        )
    }


    companion object {
        private const val PREF_KEY = "pref_show_disclaimer_message_4_2021"
    }

}
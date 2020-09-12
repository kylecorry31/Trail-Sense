package com.kylecorry.trail_sense.shared

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class DisclaimerMessage(private val context: Context) {

    fun shouldShow(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_KEY, true)
    }

    fun show() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putBoolean(PREF_KEY, false) }

        UiUtils.alert(context, context.getString(R.string.disclaimer_message_title), context.getString(
                    R.string.disclaimer_message_content), R.string.dialog_ok)
    }


    companion object {
        private const val PREF_KEY = "pref_show_disclaimer_message"
    }

}
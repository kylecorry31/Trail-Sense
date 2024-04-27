package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.licenses.Licenses
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class ShowDisclaimerCommand(private val context: Context) : Command {
    override fun execute() {
        // Shown value was previously considered to be false, but this was changed to true to make it clearer
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        if (prefs.getBoolean(context.getString(R.string.pref_main_disclaimer_shown_key)) == false) {
            prefs.putBoolean(context.getString(R.string.pref_main_disclaimer_shown_key), true)
        }

        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.app_disclaimer_message_title),
            context.getString(R.string.disclaimer_message_content) + "\n\n" + Licenses.mit(
                "2020-2024",
                "Kyle Corry"
            ),
            context.getString(R.string.pref_main_disclaimer_shown_key),
            considerShownIfCancelled = true,
            cancelText = null,
            okText = context.getString(R.string.i_agree),
            shownValue = true
        )
    }
}
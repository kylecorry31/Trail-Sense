package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command

class ShowDisclaimerCommand(private val context: Context): Command {
    override fun execute() {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.app_disclaimer_message_title),
            context.getString(R.string.disclaimer_message_content),
            context.getString(R.string.pref_main_disclaimer_shown_key),
            considerShownIfCancelled = true,
            shownValue = false
        )
    }
}
package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.licenses.Licenses
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command

class ShowDisclaimerCommand(private val context: Context) : Command {
    override fun execute() {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.app_disclaimer_message_title),
            context.getString(R.string.disclaimer_message_content) + "\n\n" + Licenses.mit(
                "2020-2023",
                "Kyle Corry"
            ),
            context.getString(R.string.pref_main_disclaimer_shown_key),
            considerShownIfCancelled = true,
            cancelText = null,
            okText = context.getString(R.string.i_agree),
            shownValue = false
        )
    }
}
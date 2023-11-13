package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.AppState
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class SettingsMoveNotice(private val context: Context) : Command {

    private val key = "settings_move_notice_shown"
    private val prefs = PreferencesSubsystem.getInstance(context).preferences

    override fun execute() {
        if (!AppState.isReturningUser) {
            prefs.putBoolean(key, true)
            return
        }

        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.settings_have_moved),
            context.getString(R.string.settings_have_moved_message),
            key,
            cancelText = null
        )
    }
}
package com.kylecorry.trail_sense.navigation.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import java.time.Duration

class ChangeBacktrackFrequencyCommand(
    private val context: Context,
    private val onChange: (Duration) -> Unit
) : Command {
    private val prefs by lazy { UserPreferences(context) }
    override fun execute() {
        val title = context.getString(R.string.pref_backtrack_frequency_title)
        CustomUiUtils.pickDuration(
            context,
            prefs.backtrackRecordFrequency,
            title,
            context.getString(R.string.actual_frequency_disclaimer),
            hint = context.getString(R.string.frequency),
            showSeconds = true
        ) {
            if (it != null && !it.isZero) {
                prefs.backtrackRecordFrequency = it
                onChange(it)
                BacktrackScheduler.restart(context)
                if (it < Duration.ofMinutes(15)) {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.battery_warning),
                        context.getString(R.string.backtrack_battery_warning),
                        cancelText = null
                    )
                }

            }
        }
    }
}
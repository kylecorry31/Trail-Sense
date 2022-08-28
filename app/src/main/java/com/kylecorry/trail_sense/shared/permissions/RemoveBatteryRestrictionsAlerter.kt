package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.IAlerter

class RemoveBatteryRestrictionsAlerter(private val context: Context) : IAlerter {

    override fun alert() {
        Alerts.dialog(
            context,
            context.getString(R.string.remove_battery_restrictions),
            MarkdownService(context).toMarkdown(context.getString(R.string.battery_usage_restricted_benefit)),
            allowLinks = true
        ) { cancelled ->
            if (!cancelled) {
                RemoveBatteryRestrictionsCommand(context).execute()
            }
        }
    }

}
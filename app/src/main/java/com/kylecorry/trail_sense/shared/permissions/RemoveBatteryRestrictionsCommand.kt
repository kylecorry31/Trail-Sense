package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Android
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryService

class RemoveBatteryRestrictionsCommand(
    private val context: Context,
    private val onlyOnAndroid12: Boolean,
    private val onlyIfServicesActive: Boolean
) : Command {

    private val batteryService = BatteryService()

    override fun execute() {
        if (onlyOnAndroid12 && Android.sdk < Build.VERSION_CODES.S) {
            return
        }

        if (Permissions.isIgnoringBatteryOptimizations(context)) {
            return
        }

        if (onlyIfServicesActive) {
            val services = batteryService.getRunningServices(context, foregroundOnly = true)
            if (services.isEmpty()) {
                return
            }
        }

        // TODO: Extract the diagnostic messages and use that
        val backtrack = context.getString(R.string.backtrack)
        val weather = context.getString(R.string.weather)
        val pedometer = context.getString(R.string.pedometer)
        val affectedTools =
            listOf(backtrack, pedometer, weather).joinToString("\n") { "- $it" }

        val message = context.getString(
            R.string.diagnostic_message_template,
            context.getString(R.string.error),
            context.getString(R.string.battery_usage_restricted),
            affectedTools,
            context.getString(R.string.battery_restricted_resolution)
        )

        Alerts.dialog(
            context,
            context.getString(R.string.diagnostics),
            MarkdownService(context).toMarkdown(message),
            okText = context.getString(R.string.settings)
        ) { cancelled ->
            if (!cancelled) {
                context.startActivity(Intents.batteryOptimizationSettings())
            }
        }

    }
}
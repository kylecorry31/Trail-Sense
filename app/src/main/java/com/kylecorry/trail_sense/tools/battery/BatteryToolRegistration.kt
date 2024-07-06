package com.kylecorry.trail_sense.tools.battery

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.battery.quickactions.QuickActionLowPowerMode
import com.kylecorry.trail_sense.tools.battery.services.BatteryLogToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object BatteryToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.BATTERY,
            context.getString(R.string.tool_battery_title),
            R.drawable.ic_tool_battery,
            R.id.fragmentToolBattery,
            ToolCategory.Power,
            guideId = R.raw.guide_tool_battery,
            settingsNavAction = R.id.powerSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_LOW_POWER_MODE,
                    context.getString(R.string.pref_low_power_mode_title),
                    ::QuickActionLowPowerMode
                )
            ),
            services = listOf(BatteryLogToolService(context)),
            diagnostics = listOf(
                ToolDiagnosticFactory.battery(context),
            ),
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_POWER_SAVING_MODE_ENABLED,
                    "Power saving mode enabled"
                ),
                ToolBroadcast(
                    BROADCAST_POWER_SAVING_MODE_DISABLED,
                    "Power saving mode disabled"
                )
            )
        )
    }

    const val BROADCAST_POWER_SAVING_MODE_ENABLED = "battery-broadcast-power-saving-mode-enabled"
    const val BROADCAST_POWER_SAVING_MODE_DISABLED = "battery-broadcast-power-saving-mode-disabled"

    const val SERVICE_BATTERY_LOG = "battery-service-battery-log"
}
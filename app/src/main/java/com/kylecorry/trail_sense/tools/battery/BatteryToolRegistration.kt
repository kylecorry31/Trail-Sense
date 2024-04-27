package com.kylecorry.trail_sense.tools.battery

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.battery.quickactions.QuickActionLowPowerMode
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.ui.Tools

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
            )
        )
    }
}
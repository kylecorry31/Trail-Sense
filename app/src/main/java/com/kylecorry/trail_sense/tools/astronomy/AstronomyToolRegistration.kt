package com.kylecorry.trail_sense.tools.astronomy

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionNightMode
import com.kylecorry.trail_sense.tools.astronomy.quickactions.QuickActionSunsetAlert
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object AstronomyToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.ASTRONOMY,
            context.getString(R.string.astronomy),
            R.drawable.ic_astronomy,
            R.id.action_astronomy,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_astronomy,
            settingsNavAction = R.id.astronomySettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_SUNSET_ALERT,
                    context.getString(R.string.sunset_alerts),
                    ::QuickActionSunsetAlert
                ),
                ToolQuickAction(
                    Tools.QUICK_ACTION_NIGHT_MODE,
                    context.getString(R.string.night),
                    ::QuickActionNightMode
                )
            )
        )
    }
}
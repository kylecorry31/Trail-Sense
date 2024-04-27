package com.kylecorry.trail_sense.tools.ruler

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.ruler.quickactions.QuickActionRuler
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object RulerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.RULER,
            context.getString(R.string.tool_ruler_title),
            R.drawable.ruler,
            R.id.rulerFragment,
            ToolCategory.Distance,
            guideId = R.raw.guide_tool_ruler,
            settingsNavAction = R.id.toolRulerSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_RULER,
                    context.getString(R.string.tool_ruler_title),
                    ::QuickActionRuler
                )
            )
        )
    }
}
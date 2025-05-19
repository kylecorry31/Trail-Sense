package com.kylecorry.trail_sense.tools.convert

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.convert.quickactions.QuickActionConvert
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object ConvertToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CONVERT,
            context.getString(R.string.convert),
            R.drawable.ic_tool_distance_convert,
            R.id.toolConvertFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_convert,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_CONVERT,
                    context.getString(R.string.convert),
                    ::QuickActionConvert
                )
            )
        )
    }
}
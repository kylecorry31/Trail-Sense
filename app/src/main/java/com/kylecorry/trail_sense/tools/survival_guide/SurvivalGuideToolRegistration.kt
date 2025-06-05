package com.kylecorry.trail_sense.tools.survival_guide

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.survival_guide.quickactions.QuickActionSurvivalGuide
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object SurvivalGuideToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SURVIVAL_GUIDE,
            context.getString(R.string.survival_guide),
            R.drawable.survival_guide,
            R.id.fragmentToolSurvivalGuideList,
            ToolCategory.Books,
            guideId = R.raw.guide_tool_survival_guide,
            additionalNavigationIds = listOf(R.id.fragmentToolSurvivalGuideReader),
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_SURVIVAL_GUIDE,
                    context.getString(R.string.survival_guide),
                    ::QuickActionSurvivalGuide
                )
            )
        )
    }
}

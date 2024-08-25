package com.kylecorry.trail_sense.tools.survival_guide

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object SurvivalGuideRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SURVIVAL_GUIDE,
            context.getString(R.string.survival_guide),
            R.drawable.ic_user_guide, // TODO: Use a different icon
            R.id.fragmentToolSurvivalGuideList,
            ToolCategory.Other,
            additionalNavigationIds = listOf(R.id.fragmentToolSurvivalGuideReader)
        )
    }
}
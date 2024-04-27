package com.kylecorry.trail_sense.tools.clouds

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.clouds.quickactions.QuickActionScanCloud
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object CloudsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CLOUDS,
            context.getString(R.string.clouds),
            R.drawable.ic_tool_clouds,
            R.id.cloudFragment,
            ToolCategory.Weather,
            guideId = R.raw.guide_tool_clouds,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_SCAN_CLOUD,
                    context.getString(R.string.cloud_scanner),
                    ::QuickActionScanCloud
                )
            ),
            additionalNavigationIds = listOf(
                R.id.cloudResultsFragment
            )
        )
    }
}
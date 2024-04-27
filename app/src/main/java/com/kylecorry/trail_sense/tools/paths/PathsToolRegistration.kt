package com.kylecorry.trail_sense.tools.paths

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.quickactions.QuickActionBacktrack
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object PathsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PATHS,
            context.getString(R.string.paths),
            R.drawable.ic_tool_backtrack,
            R.id.fragmentBacktrack,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_paths,
            settingsNavAction = R.id.pathsSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_BACKTRACK,
                    context.getString(R.string.backtrack),
                    ::QuickActionBacktrack
                )
            ),
            additionalNavigationIds = listOf(
                R.id.pathDetailsFragment
            )
        )
    }
}
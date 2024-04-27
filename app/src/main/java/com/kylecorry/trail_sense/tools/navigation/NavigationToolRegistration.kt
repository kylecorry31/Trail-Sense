package com.kylecorry.trail_sense.tools.navigation

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object NavigationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.NAVIGATION,
            context.getString(R.string.navigation),
            R.drawable.ic_compass_icon,
            R.id.action_navigation,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_navigation,
            settingsNavAction = R.id.navigationSettingsFragment
        )
    }
}
package com.kylecorry.trail_sense.tools.dashboard

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object DashboardToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.DASHBOARD,
            context.getString(R.string.tool_dashboard_title),
            R.drawable.ic_tool_dashboard,
            R.id.toolDashboardFragment,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_dashboard
        )
    }
}

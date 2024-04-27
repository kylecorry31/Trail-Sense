package com.kylecorry.trail_sense.tools.tides

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object TidesToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.TIDES,
            context.getString(R.string.tides),
            R.drawable.ic_tide_table,
            R.id.tidesFragment,
            ToolCategory.Time,
            guideId = R.raw.guide_tool_tides,
            settingsNavAction = R.id.tideSettingsFragment,
            additionalNavigationIds = listOf(
                R.id.tideListFragment,
                R.id.createTideFragment
            )
        )
    }
}
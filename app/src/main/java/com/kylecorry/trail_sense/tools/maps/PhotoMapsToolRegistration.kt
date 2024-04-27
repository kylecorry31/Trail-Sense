package com.kylecorry.trail_sense.tools.maps

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object PhotoMapsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PHOTO_MAPS,
            context.getString(R.string.photo_maps),
            R.drawable.maps,
            R.id.mapListFragment,
            ToolCategory.Location,
            context.getString(R.string.photo_map_summary),
            guideId = R.raw.guide_tool_photo_maps,
            settingsNavAction = R.id.mapSettingsFragment,
            additionalNavigationIds = listOf(
                R.id.mapsFragment
            )
        )
    }
}
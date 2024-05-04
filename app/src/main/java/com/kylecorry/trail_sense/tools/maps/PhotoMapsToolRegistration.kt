package com.kylecorry.trail_sense.tools.maps

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.maps.quickactions.QuickActionOpenPhotoMap
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
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
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_OPEN_PHOTO_MAP,
                    context.getString(R.string.open_photo_map),
                    ::QuickActionOpenPhotoMap
                )

            ),
            additionalNavigationIds = listOf(
                R.id.mapsFragment
            ),
            diagnostics2 = listOf(
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id }
        )
    }
}
package com.kylecorry.trail_sense.tools.beacons

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.beacons.quickactions.QuickActionPlaceBeacon
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object BeaconsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.BEACONS,
            context.getString(R.string.beacons),
            R.drawable.ic_location,
            R.id.beacon_list,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_beacons,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_CREATE_BEACON,
                    context.getString(R.string.create_beacon),
                    ::QuickActionPlaceBeacon
                )
            ),
            additionalNavigationIds = listOf(
                R.id.beaconDetailsFragment,
                R.id.placeBeaconFragment
            ),
            diagnostics2 = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.sightingCompass(context)
            ).distinctBy { it.id }
        )
    }
}
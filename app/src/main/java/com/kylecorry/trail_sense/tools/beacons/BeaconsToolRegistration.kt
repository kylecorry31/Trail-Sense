package com.kylecorry.trail_sense.tools.beacons

import android.content.Context
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.tools.beacons.quickactions.QuickActionPlaceBeacon
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIntentHandler
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object BeaconsToolRegistration : ToolRegistration {

    private val geoIntentHandler =
        ToolIntentHandler { activity, intent ->
            val intentData = intent.data ?: return@ToolIntentHandler false
            if (intent.scheme != "geo") return@ToolIntentHandler false
            val geo = GeoUri.from(intentData) ?: return@ToolIntentHandler false

            val bundle = bundleOf("initial_location" to geo)
            activity.findNavController().navigate(
                R.id.beacon_list,
                bundle
            )
            true
        }

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
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.sightingCompass(context)
            ).distinctBy { it.id },
            intentHandlers = listOf(geoIntentHandler)
        )
    }
}
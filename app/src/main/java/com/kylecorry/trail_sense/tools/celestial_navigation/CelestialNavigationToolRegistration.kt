package com.kylecorry.trail_sense.tools.celestial_navigation

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object CelestialNavigationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CELESTIAL_NAVIGATION,
            context.getString(R.string.celestial_navigation),
            R.drawable.ic_star,
            R.id.celestialNavigationFragment,
            ToolCategory.Location,
            context.getString(R.string.celestial_navigation_description),
            isAvailable = {
                val service = SensorService(it)
                isDebug() && service.hasCompass() && service.hasGyroscope() && Camera.hasBackCamera(
                    it
                )
            },
            // TODO: Record star
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { _, isOpen, _ -> isOpen },
                    ::SystemVolumeAction
                )
            ),
            diagnostics = listOf(
                *ToolDiagnosticFactory.sightingCompass(context),
                ToolDiagnosticFactory.gyroscope(context)
            ).distinctBy { it.id }
        )
    }
}
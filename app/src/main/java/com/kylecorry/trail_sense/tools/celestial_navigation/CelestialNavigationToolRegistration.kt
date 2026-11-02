package com.kylecorry.trail_sense.tools.celestial_navigation

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object CelestialNavigationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.CELESTIAL_NAVIGATION,
            "Celestial navigation",
            R.drawable.ic_star,
            R.id.celestialNavigationFragment,
            ToolCategory.Location,
            "Determine your location from a photo of the night sky",
            isExperimental = true,
            isAvailable = { Camera.hasBackCamera(it) }
        )
    }
}

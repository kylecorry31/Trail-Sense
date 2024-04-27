package com.kylecorry.trail_sense.settings

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.ui.Tool
import com.kylecorry.trail_sense.tools.tools.ui.ToolCategory
import com.kylecorry.trail_sense.tools.tools.ui.Tools

object SettingsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SETTINGS,
            context.getString(R.string.settings),
            R.drawable.ic_settings,
            R.id.action_settings,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_settings,
            additionalNavigationIds = listOf(
                R.id.unitSettingsFragment,
                R.id.privacySettingsFragment,
                R.id.experimentalSettingsFragment,
                R.id.errorSettingsFragment,
                R.id.sensorSettingsFragment,
                R.id.licenseFragment,
                R.id.cellSignalSettingsFragment,
                R.id.calibrateCompassFragment,
                R.id.calibrateAltimeterFragment,
                R.id.calibrateGPSFragment,
                R.id.calibrateBarometerFragment,
                R.id.thermometerSettingsFragment,
                R.id.cameraSettingsFragment
            )
        )
    }
}
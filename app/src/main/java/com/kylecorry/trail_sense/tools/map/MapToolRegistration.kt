package com.kylecorry.trail_sense.tools.map

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object MapToolRegistration : ToolRegistration {

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.MAP,
            context.getString(R.string.map),
            R.drawable.maps,
            R.id.mapFragment,
            ToolCategory.Location,
            settingsNavAction = R.id.mapSettingsFragment,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id },
        )
    }
}
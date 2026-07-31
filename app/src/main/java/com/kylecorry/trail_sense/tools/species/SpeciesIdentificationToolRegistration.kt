package com.kylecorry.trail_sense.tools.species

import android.content.Context
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object SpeciesIdentificationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SPECIES_IDENTIFICATION,
            context.getString(R.string.tool_species_identification_title),
            R.drawable.paw,
            R.id.speciesIdentificationFragment,
            ToolCategory.Books,
            description = context.getString(R.string.tool_species_identification_description),
            guideId = R.raw.guide_tool_species_identification,
            isAvailable = { Camera.hasBackCamera(it) },
            diagnostics = listOf(ToolDiagnosticFactory.camera(context))
        )
    }
}

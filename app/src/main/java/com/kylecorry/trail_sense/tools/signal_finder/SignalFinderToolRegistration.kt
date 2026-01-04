package com.kylecorry.trail_sense.tools.signal_finder

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object SignalFinderToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SIGNAL_FINDER,
            context.getString(R.string.tool_signal_finder),
            R.drawable.signal_cellular_2,
            R.id.signalFinderFragment,
            ToolCategory.Signaling,
            description = context.getString(R.string.signal_finder_description),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context)
            ),
            guideId = R.raw.guide_tool_signal_finder,
            mapLayers = listOf(
                MapLayerDefinition(
                    CellTowerMapLayer.LAYER_ID,
                    context.getString(R.string.cell_towers),
                    description = context.getString(R.string.map_layer_cell_towers_description),
                    attribution = MapLayerAttribution(
                        "[OpenCelliD Project](https://opencellid.org)",
                        context.getString(R.string.cell_tower_disclaimer),
                        true
                    ),
                ) { CellTowerMapLayer() }
            )
        )
    }
}
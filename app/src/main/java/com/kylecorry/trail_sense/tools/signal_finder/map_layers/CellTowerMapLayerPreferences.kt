package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.content.Context
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences

class CellTowerMapLayerPreferences(
    context: Context,
    mapId: String
) : BaseMapLayerPreferences(
    context,
    mapId,
    "cell_tower",
    R.string.cell_towers,
    enabledByDefault = false
) {
    override fun getAllPreferences(): List<MapLayerViewPreference> {
        val markdown = getAppService<MarkdownService>()
        return listOf(
            isEnabled.preference,
            opacity.preference,
            LabelMapLayerPreference(
                null,
                markdown.toMarkdown(context.getString(R.string.cell_tower_disclaimer))
            ),
        )
    }
}
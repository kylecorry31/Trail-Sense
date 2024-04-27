package com.kylecorry.trail_sense.shared.tiles

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class TileManager {

    fun setTilesEnabled(context: Context, enabled: Boolean) {
        val tools = Tools.getTools(context, availableOnly = false)
        tools.filter { it.tiles.any() }.forEach {
            val isAvailable = it.isAvailable(context)
            it.tiles.forEach { tile ->
                Package.setComponentEnabled(context, tile, enabled && isAvailable)
            }
        }
    }

}
package com.kylecorry.trail_sense.navigation.ui.data

import android.content.Context
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.trail_sense.navigation.ui.layers.TideLayer
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand

class UpdateTideLayerCommand(private val context: Context, private val layer: TideLayer) :
    CoroutineCommand {
    override suspend fun execute() = onDefault {
        val tables = LoadAllTideTablesCommand(context).execute()
        val currentTideCommand = CurrentTideTypeCommand(TideService())
        val tides = tables.filter { it.location != null && it.isVisible }.map {
            it to currentTideCommand.execute(it)
        }
        layer.setTides(tides)
    }
}
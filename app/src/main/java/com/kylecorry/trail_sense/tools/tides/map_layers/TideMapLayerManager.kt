package com.kylecorry.trail_sense.tools.tides.map_layers

import android.content.Context
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.tools.navigation.ui.data.UpdateTideLayerCommand
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager

class TideMapLayerManager(context: Context, layer: TideMapLayer) : BaseLayerManager() {

    private val updateTideLayerCommand = UpdateTideLayerCommand(context, layer)

    private val timer = CoroutineTimer {
        updateTideLayerCommand.execute()
    }

    override fun start() {
        timer.interval(1000)
    }

    override fun stop() {
        timer.stop()
    }
}
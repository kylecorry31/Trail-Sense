package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.navigation.ui.data.UpdateTideLayerCommand
import com.kylecorry.trail_sense.navigation.ui.layers.TideLayer

class TideLayerManager(context: Context, layer: TideLayer) : BaseLayerManager() {

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
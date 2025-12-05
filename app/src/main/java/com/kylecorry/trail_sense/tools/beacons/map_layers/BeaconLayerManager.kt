package com.kylecorry.trail_sense.tools.beacons.map_layers

import android.content.Context
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseLayerManager
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BeaconLayerManager(context: Context, private val layer: BeaconLayer) :
    BaseLayerManager() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val navigator = Navigator.Companion.getInstance(context)

    override fun start() {
        scope.launch {
            navigator.destination.collect {
                layer.highlight(it)
            }
        }
    }

    override fun stop() {
        scope.cancel()
    }
}
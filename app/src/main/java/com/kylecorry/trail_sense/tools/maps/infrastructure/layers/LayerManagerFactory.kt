package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.PathLayer

class LayerManagerFactory(private val context: Context) {

    fun getLayerManager(layer: ILayer): ILayerManager? {
        return when (layer) {
            is PathLayer -> PathLayerManager(context, layer)
            else -> null
        }
    }

}
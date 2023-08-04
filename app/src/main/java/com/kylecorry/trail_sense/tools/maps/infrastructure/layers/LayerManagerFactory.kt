package com.kylecorry.trail_sense.tools.maps.infrastructure.layers

import android.content.Context
import com.kylecorry.trail_sense.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.navigation.ui.layers.MyAccuracyLayer
import com.kylecorry.trail_sense.navigation.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.navigation.ui.layers.TideLayer
import com.kylecorry.trail_sense.shared.colors.AppColor

// TODO: Make this injectable, so it can be customized per use case
class LayerManagerFactory(private val context: Context) {

    fun getLayerManager(layer: ILayer): ILayerManager? {
        return when (layer) {
            is PathLayer -> PathLayerManager(context, layer)
            is MyLocationLayer -> MyLocationLayerManager(layer, AppColor.Orange.color)
            is MyAccuracyLayer -> MyAccuracyLayerManager(layer, AppColor.Orange.color)
            is TideLayer -> TideLayerManager(context, layer)
            else -> null
        }
    }

}
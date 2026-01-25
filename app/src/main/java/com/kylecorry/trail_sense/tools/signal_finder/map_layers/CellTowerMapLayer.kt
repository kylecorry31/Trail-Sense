package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class CellTowerMapLayer : GeoJsonLayer<CellTowerGeoJsonSource>(
    CellTowerGeoJsonSource(),
    minZoomLevel = 11,
    layerId = LAYER_ID,
    refreshOnZoom = true
) {

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        if (source.featureName == null) {
            source.featureName = context.getString(R.string.cell_tower)
        }
        super.draw(context, drawer, map)
    }

    companion object {
        const val LAYER_ID = "cell_tower"
    }
}

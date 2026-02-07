package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import android.content.Context
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class CellTowerMapLayer : GeoJsonLayer<CellTowerGeoJsonSource>(
    CellTowerGeoJsonSource(),
    minZoomLevel = 11,
    layerId = CellTowerGeoJsonSource.SOURCE_ID
) {

    override fun draw(context: Context, drawer: ICanvasDrawer, map: IMapView) {
        source.context = context
        super.draw(context, drawer, map)
    }

}

package com.kylecorry.trail_sense.tools.signal_finder.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

class CellTowerMapLayer : GeoJsonLayer<CellTowerGeoJsonSource>(
    CellTowerGeoJsonSource(),
    CellTowerGeoJsonSource.SOURCE_ID,
    minZoomLevel = 11
)

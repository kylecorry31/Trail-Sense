package com.kylecorry.trail_sense.tools.astronomy.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import java.time.Duration

class SolarEclipseLayer : TileMapLayer<SolarEclipseTileSource>(
    SolarEclipseTileSource(),
    SolarEclipseTileSource.SOURCE_ID,
    isTimeDependent = true,
    refreshInterval = Duration.ofMinutes(2)
)

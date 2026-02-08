package com.kylecorry.trail_sense.tools.astronomy.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer
import java.time.Duration

class NightLayer : TileMapLayer<NightTileSource>(
    NightTileSource(),
    NightTileSource.SOURCE_ID,
    isTimeDependent = true,
    refreshInterval = Duration.ofMinutes(1)
)

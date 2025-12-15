package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Build
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.setBlendMode
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask2
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class HillshadeLayer(taskRunner: MapLayerBackgroundTask2 = MapLayerBackgroundTask2()) :
    TileMapLayer<HillshadeMapTileSource>(HillshadeMapTileSource(), taskRunner, minZoomLevel = 10) {

    init {
        preRenderBitmaps = true
        alpha = 127
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tilePaint.setBlendMode(BlendModeCompat.MULTIPLY)
        }
    }

    fun setPreferences(prefs: HillshadeMapLayerPreferences) {
        alpha = SolMath.map(
            prefs.opacity.get() / 100f,
            0f,
            1f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
        invalidate()
    }
}
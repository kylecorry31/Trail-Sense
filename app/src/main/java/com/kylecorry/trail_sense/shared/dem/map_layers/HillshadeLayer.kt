package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Build
import android.os.Bundle
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.setBlendMode
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class HillshadeLayer(taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    TileMapLayer<HillshadeMapTileSource>(HillshadeMapTileSource(), taskRunner, minZoomLevel = 10) {

    override val layerId: String = LAYER_ID

    init {
        preRenderBitmaps = true
        alpha = 127
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tilePaint.setBlendMode(BlendModeCompat.MULTIPLY)
        }
    }

    override fun setPreferences(preferences: Bundle) {
        alpha = SolMath.map(
            preferences.getInt(DefaultMapLayerDefinitions.OPACITY) / 100f,
            0f,
            1f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
    }

    companion object {
        const val LAYER_ID = "hillshade"
    }
}
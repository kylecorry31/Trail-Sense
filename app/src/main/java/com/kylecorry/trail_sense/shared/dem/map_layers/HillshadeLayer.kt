package com.kylecorry.trail_sense.shared.dem.map_layers

import android.os.Bundle
import com.kylecorry.sol.math.SolMath
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class HillshadeLayer :
    TileMapLayer<HillshadeMapTileSource>(HillshadeMapTileSource(), minZoomLevel = 10) {

    override val layerId: String = LAYER_ID

    override val isTimeDependent: Boolean = true

    init {
        shouldMultiply = true
    }

    override fun setPreferences(preferences: Bundle) {
        super.setPreferences(preferences)
        multiplyAlpha = SolMath.map(
            preferences.getInt(
                DefaultMapLayerDefinitions.OPACITY,
                DefaultMapLayerDefinitions.DEFAULT_OPACITY
            ) / 100f,
            0f,
            1f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
    }

    companion object {
        const val LAYER_ID = "hillshade"
        const val DRAW_ACCURATE_SHADOWS = "draw_accurate_shadows"
        const val HIGH_RESOLUTION = "high_resolution"
        const val MULTI_DIRECTION_SHADING = "multi_direction_shading"
        const val DEFAULT_DRAW_ACCURATE_SHADOWS = false
        const val DEFAULT_HIGH_RESOLUTION = false
        const val DEFAULT_MULTI_DIRECTION_SHADING = false
    }
}

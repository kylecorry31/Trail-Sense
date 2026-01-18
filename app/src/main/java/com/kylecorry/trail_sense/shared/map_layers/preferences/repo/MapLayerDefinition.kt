package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer

enum class MapLayerType {
    Overlay,
    Feature,
    Tile
}

data class MapLayerAttribution(
    /**
     * The attribution text shown on the map / preference page. Should be short - ideally just the name of the copyright holder.
     * Markdown supported.
     */
    val attribution: String,
    /**
     * The long attribution text shown on preferences page.
     * Markdown supported.
     */
    val longAttribution: String? = null,
    /**
     * If true it will show up on the map.
     */
    val alwaysShow: Boolean = false
)

data class MapLayerDefinition(
    val id: String,
    val name: String,
    val preferences: List<MapLayerPreference> = emptyList(),
    val isConfigurable: Boolean = true,
    val layerType: MapLayerType = MapLayerType.Feature,
    val attribution: MapLayerAttribution? = null,
    val description: String? = null,
    val openFeature: ((feature: GeoJsonFeature, fragment: Fragment) -> Unit)? = null,
    val create: (context: Context) -> ILayer
)


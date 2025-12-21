package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.ListMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.BaseMapLayerPreferences
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class ElevationMapLayerPreferences(
    context: Context,
    mapId: String,
    isEnabledByDefault: Boolean = false
) : BaseMapLayerPreferences(
    context,
    mapId,
    "elevation",
    R.string.elevation,
    isEnabledByDefault,
    50
) {

    private var _colorStrategy by StringEnumPreference(
        cache,
        "pref_${mapId}_${layerId}_layer_color",
        ElevationColorStrategy.entries.associateBy { it.id.toString() },
        ElevationColorStrategy.USGS
    )

    val colorStrategy = MapLayerPreferenceConfig(
        get = { _colorStrategy },
        set = { _colorStrategy = it },
        preference = ListMapLayerPreference(
            context.getString(R.string.color),
            "${layerId}_layer_color",
            listOf(
                context.getString(R.string.color_usgs) to ElevationColorStrategy.USGS.id.toString(),
                context.getString(R.string.color_grayscale) to ElevationColorStrategy.Grayscale.id.toString(),
                context.getString(R.string.color_muted) to ElevationColorStrategy.Muted.id.toString(),
                context.getString(R.string.color_vibrant) to ElevationColorStrategy.Vibrant.id.toString(),
                context.getString(R.string.color_viridis) to ElevationColorStrategy.Viridis.id.toString(),
                context.getString(R.string.color_inferno) to ElevationColorStrategy.Inferno.id.toString(),
                context.getString(R.string.color_plasma) to ElevationColorStrategy.Plasma.id.toString(),
            ),
            dependency = enabledPreferenceId,
            defaultValue = ElevationColorStrategy.USGS.id.toString()
        )
    )

    override fun addPreferencesToBundle(bundle: Bundle) {
        bundle.putLong(COLOR_STRATEGY_ID, colorStrategy.get().id)
    }

    override fun setPreferencesFromBundle(bundle: Bundle) {
        val colorId = bundle.getLong(COLOR_STRATEGY_ID, ElevationColorStrategy.USGS.id)
        val color = ElevationColorStrategy.entries.firstOrNull { it.id == colorId }
            ?: ElevationColorStrategy.USGS
        colorStrategy.set(color)
    }

    override fun getAllPreferences(): List<MapLayerViewPreference> {
        return listOf(
            isEnabled.preference,
            LabelMapLayerPreference(
                context.getString(R.string.plugin_digital_elevation_model),
                context.getString(R.string.open_settings)
            ) { ctx ->
                if (ctx is MainActivity) {
                    ctx.findNavController()
                        .navigateWithAnimation(R.id.calibrateAltimeterFragment)
                }
            },
            opacity.preference,
            colorStrategy.preference
        )
    }

    companion object {
        const val COLOR_STRATEGY_ID = "colorStrategyId"
    }
}
package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerPreferenceConfig
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference

abstract class BaseMapLayerPreferences(
    context: Context,
    protected val mapId: String,
    protected val layerId: String,
    @StringRes nameResId: Int,
    enabledByDefault: Boolean = true,
    defaultOpacityPercent: Int = 100,
    dependency: String? = null,
) :
    PreferenceRepo(context) {

    protected val enabledPreferenceId = "${layerId}_layer_enabled"
    protected val name = context.getString(nameResId)

    private var _isEnabled by BooleanPreference(
        cache,
        "pref_${mapId}_${enabledPreferenceId}",
        enabledByDefault
    )

    val isEnabled = MapLayerPreferenceConfig(
        get = { _isEnabled },
        set = { _isEnabled = it },
        preference = SwitchMapLayerPreference(
            name,
            enabledPreferenceId,
            defaultValue = enabledByDefault,
            dependency = dependency
        )
    )

    private var _opacity by IntPreference(
        cache,
        "pref_${mapId}_${layerId}_layer_opacity",
        defaultOpacityPercent
    )

    val opacity = MapLayerPreferenceConfig(
        get = { _opacity },
        set = { _opacity = it },
        preference = SeekbarMapLayerPreference(
            context.getString(R.string.opacity),
            "${layerId}_layer_opacity",
            defaultValue = defaultOpacityPercent,
            dependency = enabledPreferenceId
        )
    )

    fun getPreferences(): MapLayerViewPreferences {
        return MapLayerViewPreferences(
            layerId,
            name,
            getAllPreferences()
        )
    }

    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putBoolean(IS_ENABLED, isEnabled.get())
        bundle.putInt(OPACITY, opacity.get())
        addPreferencesToBundle(bundle)
        return bundle
    }

    open fun addPreferencesToBundle(bundle: Bundle) {
        // Do nothing
    }

    protected open fun getAllPreferences(): List<MapLayerViewPreference> {
        return listOf(
            isEnabled.preference,
            opacity.preference
        )
    }

    companion object {
        const val IS_ENABLED = "isEnabled"
        const val OPACITY = "opacity"
    }
}
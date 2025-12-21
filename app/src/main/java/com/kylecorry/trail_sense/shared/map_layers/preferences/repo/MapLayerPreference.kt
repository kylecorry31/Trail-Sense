package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.LabelMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.ListMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.MapLayerViewPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SeekbarMapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.definition.SwitchMapLayerPreference
import com.kylecorry.trail_sense.shared.navigateWithAnimation

enum class MapLayerPreferenceType {
    Label,
    Enum,
    Seekbar,
    Switch
}

data class MapLayerPreference(
    val id: String,
    val title: CharSequence?,
    val type: MapLayerPreferenceType,
    val summary: CharSequence? = null,
    val defaultValue: Any? = null,
    val dependency: String? = DefaultMapLayerDefinitions.ENABLED,
    val min: Number? = null,
    val max: Number? = null,
    val values: List<Pair<String, String>>? = null,
    // TODO: This isn't the right thing to do
    val openDemSettingsOnClick: Boolean = false
)

interface MapLayerViewPreferenceConverter {
    fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference
}

class LabelMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return LabelMapLayerPreference(
            preference.title,
            preference.summary,
            preference.getDependencyBasePreferenceKey(layerId)
        ) { context ->
            if (preference.openDemSettingsOnClick && context is MainActivity) {
                context.findNavController()
                    .navigateWithAnimation(R.id.calibrateAltimeterFragment)
            }
        }
    }
}

class EnumMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return ListMapLayerPreference(
            preference.title?.toString() ?: "",
            preference.getBasePreferenceKey(layerId),
            preference.values ?: emptyList(),
            preference.defaultValue as? String?,
            preference.getDependencyBasePreferenceKey(layerId)
        )
    }
}

class SeekbarMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return SeekbarMapLayerPreference(
            preference.title?.toString() ?: "",
            preference.getBasePreferenceKey(layerId),
            (preference.defaultValue as? Int) ?: 0,
            preference.min?.toInt() ?: 0,
            preference.max?.toInt() ?: 100,
            preference.getDependencyBasePreferenceKey(layerId)
        )
    }
}

class SwitchMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(preference: MapLayerPreference, layerId: String): MapLayerViewPreference {
        return SwitchMapLayerPreference(
            preference.title?.toString() ?: "",
            preference.getBasePreferenceKey(layerId),
            (preference.defaultValue as? Boolean) ?: true,
            preference.getDependencyBasePreferenceKey(layerId),
            preference.summary?.toString()
        )
    }

}

class MapLayerViewPreferenceConverterFactory {
    fun getConverter(preferenceType: MapLayerPreferenceType): MapLayerViewPreferenceConverter {
        return when (preferenceType) {
            MapLayerPreferenceType.Label -> LabelMapLayerViewPreferenceConverter()
            MapLayerPreferenceType.Enum -> EnumMapLayerViewPreferenceConverter()
            MapLayerPreferenceType.Seekbar -> SeekbarMapLayerViewPreferenceConverter()
            MapLayerPreferenceType.Switch -> SwitchMapLayerViewPreferenceConverter()
        }
    }
}

private fun MapLayerPreference.getDependencyBasePreferenceKey(layerId: String): String? {
    if (dependency == null) {
        return null
    }
    return "${layerId}_layer_${dependency}"
}

private fun MapLayerPreference.getBasePreferenceKey(layerId: String): String {
    return "${layerId}_layer_${id}"
}

fun MapLayerPreference.getFullPreferenceKey(mapId: String, layerId: String): String {
    return "pref_${mapId}_${getBasePreferenceKey(layerId)}"
}
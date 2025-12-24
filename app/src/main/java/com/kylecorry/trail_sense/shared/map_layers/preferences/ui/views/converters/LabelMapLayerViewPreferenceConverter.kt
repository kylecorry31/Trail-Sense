package com.kylecorry.trail_sense.shared.map_layers.preferences.ui.views.converters

import android.content.Context
import androidx.preference.Preference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getDependencyBasePreferenceKey
import com.kylecorry.trail_sense.shared.navigateWithAnimation

class LabelMapLayerViewPreferenceConverter : MapLayerViewPreferenceConverter {
    override fun convert(
        context: Context,
        mapId: String,
        layerId: String,
        preference: MapLayerPreference
    ): Preference {
        val androidPreference = Preference(context)
        androidPreference.isIconSpaceReserved = false
        androidPreference.isSingleLineTitle = false
        androidPreference.title = preference.title
        androidPreference.summary = preference.summary
        androidPreference.key = preference.getFullPreferenceKey(mapId, layerId)
        androidPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // TODO: Find a better way to do this
            if (preference.openDemSettingsOnClick && context is MainActivity) {
                context.findNavController()
                    .navigateWithAnimation(R.id.calibrateAltimeterFragment)
            }
            true
        }
        return androidPreference
    }
}
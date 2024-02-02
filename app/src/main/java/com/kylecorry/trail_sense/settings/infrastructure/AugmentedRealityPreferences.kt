package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.R

class AugmentedRealityPreferences(context: Context) : PreferenceRepo(context) {

    var beaconViewDistance by FloatPreference(
        cache,
        context.getString(R.string.pref_augmented_reality_view_distance),
        1000f
    )

    var pathViewDistance by FloatPreference(
        cache,
        context.getString(R.string.pref_augmented_reality_view_distance_paths),
        250f
    )

}
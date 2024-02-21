package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.R

class AugmentedRealityPreferences(context: Context) : PreferenceRepo(context) {

    var beaconViewDistance by FloatPreference(
        cache,
        context.getString(R.string.pref_augmented_reality_view_distance),
        1000f
    )

    var showPathLayer by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_ar_path_layer),
        true
    )

    var pathViewDistance by FloatPreference(
        cache,
        context.getString(R.string.pref_augmented_reality_view_distance_paths),
        20f
    )

    val useGyroOnlyAfterCalibration by BooleanPreference(
        cache,
        context.getString(R.string.pref_ar_use_gyro_only_after_calibration),
        false
    )
}
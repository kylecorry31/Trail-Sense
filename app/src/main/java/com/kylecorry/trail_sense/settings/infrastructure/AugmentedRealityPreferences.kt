package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

class AugmentedRealityPreferences(context: Context) : PreferenceRepo(context) {

    var beaconViewDistance by FloatPreference(
        cache,
        context.getString(R.string.pref_augmented_reality_view_distance),
        1000f
    )

    val maxRecommendedPathViewDistanceMeters by lazy {
        if (UserPreferences(context).baseDistanceUnits == DistanceUnits.Meters) {
            30f
        } else {
            Distance.feet(100f).meters().distance
        }
    }

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

    val adjustForPathElevation by BooleanPreference(
        cache,
        context.getString(R.string.pref_ar_adjust_for_path_elevation),
        false
    )

    // Layer visibility
    var showBeaconLayer by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_ar_beacon_layer),
        true
    )

    var showPathLayer by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_ar_path_layer),
        true
    )

    var showAstronomyLayer by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_ar_astronomy_layer),
        true
    )

    var showGridLayer by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_ar_grid_layer),
        true
    )

    var showSatelliteLayer = false

    var showStars by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_ar_stars),
        true
    )
}
package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.R

class CameraPreferences(context: Context) : PreferenceRepo(context), ICameraPreferences {

    override val useZeroShutterLag by BooleanPreference(
        cache,
        context.getString(R.string.pref_use_zero_shutter_lag),
        false
    )

    override val projectionType by StringEnumPreference(
        cache,
        context.getString(R.string.pref_augmented_reality_mapper),
        ProjectionType.entries.associateBy { it.id },
        ProjectionType.EstimatedIntrinsics
    )

    enum class ProjectionType(val id: String) {
        EstimatedIntrinsics("estimated_intrinsics"),
        ManufacturerIntrinsics("manufacturer_intrinsics"),
        Perspective("perspective"),
        Linear("linear")
    }

}
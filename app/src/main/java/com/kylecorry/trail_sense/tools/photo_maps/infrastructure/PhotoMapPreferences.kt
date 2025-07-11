package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.ContourMapLayerPreferences
import com.kylecorry.trail_sense.tools.photo_maps.domain.sort.MapSortMethod

class PhotoMapPreferences(context: Context) : PreferenceRepo(context) {
    val autoReducePhotoMaps by BooleanPreference(
        cache,
        context.getString(R.string.pref_low_resolution_maps),
        true
    )

    val autoReducePdfMaps by BooleanPreference(
        cache,
        context.getString(R.string.pref_low_resolution_pdf_maps),
        true
    )

    val showMapPreviews by BooleanPreference(
        cache,
        context.getString(R.string.pref_show_map_previews),
        true
    )

    var mapSort: MapSortMethod by IntEnumPreference(
        cache,
        context.getString(R.string.pref_map_sort),
        MapSortMethod.values().associateBy { it.id.toInt() },
        MapSortMethod.MostRecent
    )

    val keepMapFacingUp by BooleanPreference(
        cache,
        context.getString(R.string.pref_keep_map_facing_up),
        true
    )

    val keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        context.getString(R.string.pref_photo_maps_keep_unlocked),
        false
    )

    // Layers
    val contourLayer = ContourMapLayerPreferences(context, "photo_maps")
}
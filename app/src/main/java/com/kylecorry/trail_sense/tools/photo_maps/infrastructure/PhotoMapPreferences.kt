package com.kylecorry.trail_sense.tools.photo_maps.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayerPreferenceManager
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconMapLayerPreferences
import com.kylecorry.trail_sense.tools.photo_maps.domain.sort.MapSortMethod
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerPreferences
import com.kylecorry.trail_sense.tools.paths.map_layers.PathMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationMapLayerPreferences

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
    val pathLayer = PathMapLayerPreferences(context, "photo_maps")
    val beaconLayer = BeaconMapLayerPreferences(context, "photo_maps")
    val navigationLayer = NavigationMapLayerPreferences(context, "photo_maps")
    val tideLayer = TideMapLayerPreferences(context, "photo_maps")
    val myLocationLayer = MyLocationMapLayerPreferences(context, "photo_maps")
    val layerManager = MapLayerPreferenceManager(
        "photo_maps", listOf(
            contourLayer.getPreferences(),
            pathLayer.getPreferences(),
            beaconLayer.getPreferences(),
            navigationLayer.getPreferences(),
            tideLayer.getPreferences(),
            myLocationLayer.getPreferences()
        )
    )
}
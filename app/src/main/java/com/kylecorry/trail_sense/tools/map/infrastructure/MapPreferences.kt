package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourMapLayerPreferences
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayerPreferenceManager
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconMapLayerPreferences
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerPreferences
import com.kylecorry.trail_sense.tools.paths.map_layers.PathMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyAccuracyMapLayerPreferences

class MapPreferences(context: Context) : PreferenceRepo(context) {

    val keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        context.getString(R.string.pref_map_keep_unlocked),
        false
    )

    // Layers
    val baseMapLayer = BaseMapMapLayerPreferences(context, "map", defaultOpacity = 100)
    val photoMapLayer = PhotoMapMapLayerPreferences(context, "map", defaultOpacity = 100)
    val beaconLayer = BeaconMapLayerPreferences(context, "map")
    val pathLayer = PathMapLayerPreferences(context, "map")
    val navigationLayer = NavigationMapLayerPreferences(context, "map")
    val tideLayer = TideMapLayerPreferences(context, "map")
    val contourLayer = ContourMapLayerPreferences(context, "map", isEnabledByDefault = true)
    val myLocationLayer = MyLocationMapLayerPreferences(context, "map")
    val myAccuracyLayer = MyAccuracyMapLayerPreferences(context, "map")

    val layerManager = MapLayerPreferenceManager(
        "map", listOf(
            baseMapLayer.getPreferences(),
            photoMapLayer.getPreferences(),
            contourLayer.getPreferences(),
            pathLayer.getPreferences(),
            beaconLayer.getPreferences(),
            navigationLayer.getPreferences(),
            tideLayer.getPreferences(),
            myAccuracyLayer.getPreferences(),
            myLocationLayer.getPreferences()
        )
    )
}
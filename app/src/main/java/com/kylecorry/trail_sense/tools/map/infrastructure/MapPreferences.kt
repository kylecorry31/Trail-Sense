package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationMapLayerPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapLayerPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayerPreferenceManager
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationMapLayerPreferences
import com.kylecorry.trail_sense.tools.paths.map_layers.PathMapLayerPreferences
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapMapLayerPreferences
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayerPreferences
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerPreferences

class MapPreferences(context: Context) : PreferenceRepo(context) {

    val keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        context.getString(R.string.pref_map_keep_unlocked),
        false
    )

    val saveMapState by BooleanPreference(
        cache,
        context.getString(R.string.pref_save_map_state),
        false
    )

    // Layers
    private val mapId = "map"
    val baseMapLayer = BaseMapMapLayerPreferences(context, mapId)
    val photoMapLayer = PhotoMapMapLayerPreferences(context, mapId, defaultOpacity = 100)
    val beaconLayer = BeaconMapLayerPreferences(context, mapId)
    val pathLayer = PathMapLayerPreferences(context, mapId)
    val navigationLayer = NavigationMapLayerPreferences(context, mapId)
    val tideLayer = TideMapLayerPreferences(context, mapId)
    val contourLayer = ContourMapLayerPreferences(context, mapId, isEnabledByDefault = true)
    val myLocationLayer = MyLocationMapLayerPreferences(context, mapId)
    val elevationLayer = ElevationMapLayerPreferences(context, mapId, isEnabledByDefault = true)
    val hillshadeLayer = HillshadeMapLayerPreferences(context, mapId, isEnabledByDefault = true)
    val cellTowerLayer = CellTowerMapLayerPreferences(context, mapId)

    val layerManager = MapLayerPreferenceManager(
        mapId, listOf(
            baseMapLayer.getPreferences(),
            elevationLayer.getPreferences(),
            hillshadeLayer.getPreferences(),
            photoMapLayer.getPreferences(),
            contourLayer.getPreferences(),
            cellTowerLayer.getPreferences(),
            pathLayer.getPreferences(),
            beaconLayer.getPreferences(),
            navigationLayer.getPreferences(),
            tideLayer.getPreferences(),
            myLocationLayer.getPreferences()
        )
    )
}
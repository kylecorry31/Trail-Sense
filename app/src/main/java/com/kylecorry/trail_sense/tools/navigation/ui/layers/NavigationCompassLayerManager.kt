package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer

class NavigationCompassLayerManager {
    private val taskRunner = MapLayerBackgroundTask()
    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer()
    private val myLocationLayer = MyLocationLayer()
    private val tideLayer = TideMapLayer()
    private val photoMapLayer = PhotoMapLayer()
    private val contourLayer = ContourLayer(taskRunner)
    private val elevationLayer = ElevationLayer(taskRunner)
    private val hillshadeLayer = HillshadeLayer(taskRunner)
    private val cellTowerLayer = CellTowerMapLayer()
    private val prefs = AppServiceRegistry.get<UserPreferences>()

    var key = 0
        private set

    fun resume(context: Context, view: IMapView) {
        view.setLayersWithPreferences(
            elevationLayer to prefs.navigation.elevationLayer,
            hillshadeLayer to prefs.navigation.hillshadeLayer,
            photoMapLayer to prefs.navigation.photoMapLayer,
            contourLayer to prefs.navigation.contourLayer,
            cellTowerLayer to prefs.navigation.cellTowerLayer,
            pathLayer to prefs.navigation.pathLayer,
            myLocationLayer to prefs.navigation.myLocationLayer,
            tideLayer to prefs.navigation.tideLayer,
            beaconLayer to prefs.navigation.beaconLayer
        )

        key += 1

        if (prefs.navigation.useRadarCompass) {
            view.start()
        }
    }

    fun pause(view: IMapView) {
        view.stop()
    }

    fun onBearingChanged(bearing: Float) {
        myLocationLayer.setAzimuth(bearing)
    }

    fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        myLocationLayer.setLocation(location)
        myLocationLayer.setAccuracy(accuracy)
    }

    fun setDestination(beacon: Beacon?) {
        beaconLayer.highlight(beacon)
    }

}
package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MultiLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.TiledMapLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayerManager
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerManager

class NavigationCompassLayerManager {

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer()
    private val myLocationLayer = MyLocationLayer()
    private val tideLayer = TideMapLayer()
    private val photoMapLayer = TiledMapLayer()
    private val contourLayer = ContourLayer()
    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private var layerManager: ILayerManager? = null

    var key = 0
        private set

    fun resume(context: Context, view: IMapView) {
        val hasCompass = SensorService(context).hasCompass()

        if (!hasCompass) {
            myLocationLayer.setShowDirection(false)
        }

        val isMapLayerEnabled = prefs.navigation.photoMapLayer.isEnabled.get()
        val isContourLayerEnabled = prefs.navigation.contourLayer.isEnabled.get()
        val isPathLayerEnabled = prefs.navigation.pathLayer.isEnabled.get()
        val isBeaconLayerEnabled = prefs.navigation.beaconLayer.isEnabled.get()
        val isTideLayerEnabled = prefs.navigation.tideLayer.isEnabled.get()
        val isMyLocationLayerEnabled = prefs.navigation.myLocationLayer.isEnabled.get()

        beaconLayer.setOutlineColor(Resources.color(context, R.color.colorSecondary))
        beaconLayer.setPreferences(prefs.navigation.beaconLayer)
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        pathLayer.setPreferences(prefs.navigation.pathLayer)
        photoMapLayer.setPreferences(prefs.navigation.photoMapLayer)
        contourLayer.setPreferences(prefs.navigation.contourLayer)
        tideLayer.setPreferences(prefs.navigation.tideLayer)
        myLocationLayer.setPreferences(prefs.navigation.myLocationLayer)
        photoMapLayer.setBackgroundColor(Resources.color(context, R.color.colorSecondary))
        photoMapLayer.setMinZoom(4)
        photoMapLayer.controlsPdfCache = true
        view.setLayers(
            listOfNotNull(
                if (isMapLayerEnabled) photoMapLayer else null,
                if (isContourLayerEnabled) contourLayer else null,
                if (isPathLayerEnabled) pathLayer else null,
                if (isMyLocationLayerEnabled) myLocationLayer else null,
                if (isTideLayerEnabled) tideLayer else null,
                if (isBeaconLayerEnabled) beaconLayer else null
            )
        )

        layerManager = MultiLayerManager(
            listOfNotNull(
                if (isPathLayerEnabled) PathLayerManager(context, pathLayer) else null,
                if (isMyLocationLayerEnabled) MyLocationLayerManager(
                    myLocationLayer,
                    Color.WHITE,
                    Resources.getPrimaryMarkerColor(context)
                ) else null,
                if (isTideLayerEnabled) TideMapLayerManager(context, tideLayer) else null,
                if (isMapLayerEnabled) PhotoMapLayerManager(
                    context,
                    photoMapLayer,
                    loadPdfs = prefs.navigation.photoMapLayer.loadPdfs.get()
                ) else null
            )
        )

        key += 1

        if (prefs.navigation.useRadarCompass) {
            layerManager?.start()
        }
    }

    fun pause(context: Context, view: IMapView) {
        layerManager?.stop()
        layerManager = null
        PhotoMapRegionLoader.removeUnneededLoaders(emptyList())
    }

    fun onBearingChanged(bearing: Float) {
        layerManager?.onBearingChanged(bearing)
    }

    fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        layerManager?.onLocationChanged(location, accuracy)
    }

    fun onBoundsChanged(bounds: CoordinateBounds) {
        layerManager?.onBoundsChanged(bounds)
    }

    fun setBeacons(beacons: List<Beacon>) {
        beaconLayer.setBeacons(beacons)
    }

    fun setDestination(beacon: Beacon?) {
        beaconLayer.highlight(beacon)
    }

}
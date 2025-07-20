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
import com.kylecorry.trail_sense.shared.dem.ContourLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.ILayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MapLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MultiLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PathLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PhotoMapLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.TideLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader

class NavigationCompassLayerManager {

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer()
    private val myLocationLayer = MyLocationLayer()
    private val myAccuracyLayer = MyAccuracyLayer()
    private val tideLayer = TideLayer()
    private val photoMapLayer = MapLayer()
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

        val isMapLayerEnabled = prefs.navigation.photoMapLayer.isEnabled
        val isContourLayerEnabled = prefs.navigation.contourLayer.isEnabled

        beaconLayer.setOutlineColor(Resources.color(context, R.color.colorSecondary))
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        photoMapLayer.setPreferences(prefs.navigation.photoMapLayer)
        contourLayer.setPreferences(prefs.navigation.contourLayer)
        photoMapLayer.setBackgroundColor(Resources.color(context, R.color.colorSecondary))
        photoMapLayer.setMinZoom(4)
        photoMapLayer.controlsPdfCache = true
        view.setLayers(
            listOfNotNull(
                if (isMapLayerEnabled) photoMapLayer else null,
                if (isContourLayerEnabled) contourLayer else null,
                pathLayer,
                myAccuracyLayer,
                myLocationLayer,
                tideLayer,
                beaconLayer
            )
        )

        layerManager = MultiLayerManager(
            listOfNotNull(
                PathLayerManager(context, pathLayer),
                MyAccuracyLayerManager(
                    myAccuracyLayer,
                    Resources.getPrimaryMarkerColor(context),
                    25
                ),
                MyLocationLayerManager(myLocationLayer, Color.WHITE),
                TideLayerManager(context, tideLayer),
                if (isMapLayerEnabled) PhotoMapLayerManager(
                    context,
                    photoMapLayer,
                    replaceWhitePixels = true,
                    loadPdfs = prefs.navigation.photoMapLayer.loadPdfs
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
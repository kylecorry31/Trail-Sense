package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.ContourLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseMapLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.CompassOverlayLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ScaleBarLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.layers.BeaconLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyAccuracyLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.TideLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.BeaconLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.ILayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MapLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MultiLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.NavigationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PathLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PhotoMapLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.TideLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader

class MapToolLayerManager {

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer {
        val navigator = AppServiceRegistry.get<Navigator>()
        navigator.navigateTo(it)
        true
    }
    private val myLocationLayer = MyLocationLayer()
    private val myAccuracyLayer = MyAccuracyLayer()
    private val tideLayer = TideLayer()
    private val baseMapLayer = MapLayer()
    private val photoMapLayer = MapLayer()
    private val contourLayer = ContourLayer()
    private val navigationLayer = NavigationLayer()
    private val scaleBarLayer = ScaleBarLayer()
    private val backgroundLayer = BackgroundColorMapLayer()
    private var myElevationLayer: MyElevationLayer? = null
    private val compassLayer = CompassOverlayLayer()

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()
    private var layerManager: ILayerManager? = null

    var key = 0
        private set

    fun resume(context: Context, view: IMapView) {
        val hasCompass = SensorService(context).hasCompass()

        compassLayer.backgroundColor = Resources.color(context, R.color.colorSecondary)
        compassLayer.cardinalDirectionColor = Resources.getCardinalDirectionColor(context)

        myElevationLayer = MyElevationLayer(
            formatter,
            PixelCoordinate(
                Resources.dp(context, 16f),
                -Resources.dp(context, 16f)
            )
        )

        if (!hasCompass) {
            myLocationLayer.setShowDirection(false)
        }

        scaleBarLayer.units = prefs.baseDistanceUnits
        backgroundLayer.color = Color.WHITE

        beaconLayer.setOutlineColor(Resources.color(context, R.color.colorSecondary))
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        photoMapLayer.setMinZoom(4)
        photoMapLayer.controlsPdfCache = true
        photoMapLayer.setPreferences(prefs.map.photoMapLayer)
        contourLayer.setPreferences(prefs.map.contourLayer)

        photoMapLayer.setBackgroundColor(Color.TRANSPARENT)
        view.setLayers(
            listOfNotNull(
                baseMapLayer,
                if (prefs.map.photoMapLayer.isEnabled) photoMapLayer else null,
                if (prefs.map.contourLayer.isEnabled) contourLayer else null,
                navigationLayer,
                pathLayer,
                myAccuracyLayer,
                myLocationLayer,
                tideLayer,
                beaconLayer,
                scaleBarLayer,
                myElevationLayer,
                compassLayer
            )
        )

        layerManager = MultiLayerManager(
            listOfNotNull(
                PathLayerManager(context, pathLayer),
                MyAccuracyLayerManager(
                    myAccuracyLayer,
                    Resources.getPrimaryMarkerColor(context)
                ),
                MyLocationLayerManager(
                    myLocationLayer,
                    Resources.getPrimaryMarkerColor(context)
                ),
                TideLayerManager(context, tideLayer),
                if (prefs.map.photoMapLayer.isEnabled) PhotoMapLayerManager(
                    context,
                    photoMapLayer,
                    replaceWhitePixels = true,
                    loadPdfs = prefs.map.photoMapLayer.loadPdfs
                ) else null,
                BaseMapLayerManager(context, baseMapLayer),
                BeaconLayerManager(context, beaconLayer),
                NavigationLayerManager(context, navigationLayer)
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

    fun onElevationChanged(elevation: Float) {
        myElevationLayer?.elevation = Distance.meters(elevation).convertTo(prefs.baseDistanceUnits)
    }

}
package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BackgroundColorMapLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseMapLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.CompassOverlayLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MultiLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyElevationLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ScaleBarLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.TiledMapLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayerManager
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayerManager
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.PhotoMapRegionLoader
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.ui.MapDistanceLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerManager

class MapToolLayerManager {

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer {
        val navigator = AppServiceRegistry.get<Navigator>()
        navigator.navigateTo(it)
        true
    }
    private val taskRunner = MapLayerBackgroundTask()
    private val myLocationLayer = MyLocationLayer()
    private val tideLayer = TideMapLayer()
    private val baseMapLayer = TiledMapLayer()
    private val photoMapLayer = TiledMapLayer()
    private var contourLayer: ContourLayer? = null
    private var hillshadeLayer: HillshadeLayer? = null
    private var elevationLayer: ElevationLayer? = null
    private val navigationLayer = NavigationLayer()
    private val scaleBarLayer = ScaleBarLayer()
    private var myElevationLayer: MyElevationLayer? = null
    private val compassLayer = CompassOverlayLayer()
    private val selectedPointLayer = BeaconLayer()
    private val distanceLayer = MapDistanceLayer { onDistancePathChange(it) }
    private val cellTowerLayer = CellTowerMapLayer {
        CellTowerMapLayer.navigate(it)
        true
    }

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()
    private var layerManager: ILayerManager? = null
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null

    var key: Int = 0

    fun resume(context: Context, view: IMapView) {
        val hasCompass = SensorService(context).hasCompass()

        contourLayer = ContourLayer(taskRunner)
        hillshadeLayer = HillshadeLayer(taskRunner)
        elevationLayer = ElevationLayer(taskRunner)

        compassLayer.backgroundColor = Resources.color(context, R.color.colorSecondary)
        compassLayer.cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
        compassLayer.paddingTopDp = 48f
        compassLayer.paddingRightDp = 8f

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

        beaconLayer.setOutlineColor(Resources.color(context, R.color.colorSecondary))
        beaconLayer.setPreferences(prefs.map.beaconLayer)

        selectedPointLayer.setOutlineColor(Color.WHITE)

        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        pathLayer.setPreferences(prefs.map.pathLayer)

        navigationLayer.setPreferences(prefs.map.navigationLayer)

        baseMapLayer.setPreferences(prefs.map.baseMapLayer)

        photoMapLayer.setMinZoom(4)
        photoMapLayer.controlsPdfCache = true
        photoMapLayer.setPreferences(prefs.map.photoMapLayer)

        contourLayer?.setPreferences(prefs.map.contourLayer)
        elevationLayer?.setPreferences(prefs.map.elevationLayer)
        hillshadeLayer?.setPreferences(prefs.map.hillshadeLayer)

        photoMapLayer.setBackgroundColor(Color.TRANSPARENT)

        distanceLayer.isEnabled = false
        distanceLayer.setOutlineColor(Color.WHITE)
        distanceLayer.setPathColor(Color.BLACK)

        tideLayer.setPreferences(prefs.map.tideLayer)

        myLocationLayer.setPreferences(prefs.map.myLocationLayer)

        cellTowerLayer.setPreferences(prefs.map.cellTowerLayer)

        view.setLayers(
            listOfNotNull(
                BackgroundColorMapLayer().also { it.color = Color.rgb(127, 127, 127) },
                if (prefs.map.baseMapLayer.isEnabled.get()) baseMapLayer else null,
                if (prefs.map.elevationLayer.isEnabled.get()) elevationLayer else null,
                if (prefs.map.hillshadeLayer.isEnabled.get()) hillshadeLayer else null,
                if (prefs.map.photoMapLayer.isEnabled.get()) photoMapLayer else null,
                if (prefs.map.contourLayer.isEnabled.get()) contourLayer else null,
                if (prefs.map.cellTowerLayer.isEnabled.get()) cellTowerLayer else null,
                if (prefs.map.navigationLayer.isEnabled.get()) navigationLayer else null,
                if (prefs.map.pathLayer.isEnabled.get()) pathLayer else null,
                if (prefs.map.myLocationLayer.isEnabled.get()) myLocationLayer else null,
                if (prefs.map.tideLayer.isEnabled.get()) tideLayer else null,
                if (prefs.map.beaconLayer.isEnabled.get()) beaconLayer else null,
                selectedPointLayer,
                distanceLayer,

                // Overlays
                scaleBarLayer,
                myElevationLayer,
                compassLayer
            )
        )

        layerManager = MultiLayerManager(
            listOfNotNull(
                if (prefs.map.pathLayer.isEnabled.get()) PathLayerManager(
                    context,
                    pathLayer
                ) else null,
                if (prefs.map.myLocationLayer.isEnabled.get()) MyLocationLayerManager(
                    myLocationLayer,
                    Resources.getPrimaryMarkerColor(context),
                    Resources.getPrimaryMarkerColor(context)
                ) else null,
                if (prefs.map.tideLayer.isEnabled.get()) TideMapLayerManager(
                    context,
                    tideLayer
                ) else null,
                if (prefs.map.photoMapLayer.isEnabled.get()) PhotoMapLayerManager(
                    context,
                    photoMapLayer,
                    loadPdfs = prefs.map.photoMapLayer.loadPdfs.get()
                ) else null,
                if (prefs.map.baseMapLayer.isEnabled.get()) BaseMapLayerManager(
                    context,
                    baseMapLayer
                ) else null,
                if (prefs.map.beaconLayer.isEnabled.get()) BeaconLayerManager(
                    context,
                    beaconLayer
                ) else null,
                if (prefs.map.navigationLayer.isEnabled.get()) NavigationLayerManager(
                    context,
                    navigationLayer
                ) else null
            )
        )

        layerManager?.start()

        if (view is View) {
            view.invalidate()
        }

        key++
    }

    fun pause(context: Context, view: IMapView) {
        taskRunner.stop()
        layerManager?.stop()
        layerManager = null
        PhotoMapRegionLoader.removeUnneededLoaders(emptyList())
    }

    fun onBearingChanged(bearing: Bearing) {
        layerManager?.onBearingChanged(bearing.value)
    }

    fun onLocationChanged(location: Coordinate, accuracy: Distance?) {
        layerManager?.onLocationChanged(location, accuracy?.meters()?.value)
    }

    fun onBoundsChanged(bounds: CoordinateBounds) {
        layerManager?.onBoundsChanged(bounds)
        distanceLayer.invalidate()
    }

    fun onElevationChanged(elevation: Distance) {
        myElevationLayer?.elevation = elevation.convertTo(prefs.baseDistanceUnits)
    }

    fun setSelectedLocation(location: Coordinate?) {
        selectedPointLayer.setBeacons(
            listOfNotNull(
                if (location == null) {
                    null
                } else {
                    Beacon(0, "", location)
                }
            )
        )
    }

    // Distance measurement

    private fun onDistancePathChange(points: List<Coordinate>) {
        // Display distance
        val distance = Geology.getPathDistance(points)
        onDistanceChangedCallback?.invoke(distance)
    }

    fun setOnDistanceChangedCallback(callback: ((Distance) -> Unit)?) {
        onDistanceChangedCallback = callback
    }

    fun stopDistanceMeasurement() {
        distanceLayer.isEnabled = false
        distanceLayer.clear()
    }

    fun undoLastDistanceMeasurement() {
        distanceLayer.undo()
    }

    fun getDistanceMeasurementPoints(): List<Coordinate> {
        return distanceLayer.getPoints()
    }

    fun startDistanceMeasurement(vararg initialPoints: Coordinate) {
        distanceLayer.isEnabled = true
        distanceLayer.clear()
        initialPoints.forEach { distanceLayer.add(it) }
    }

    fun isMeasuringDistance(): Boolean {
        return distanceLayer.isEnabled
    }

}
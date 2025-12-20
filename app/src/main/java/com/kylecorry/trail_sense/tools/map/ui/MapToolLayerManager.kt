package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
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
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask2
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BackgroundColorMapLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.BaseMapLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.CompassOverlayLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MultiLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyElevationLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ScaleBarLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayer
import com.kylecorry.trail_sense.tools.photo_maps.ui.MapDistanceLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer

class MapToolLayerManager {

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer {
        val navigator = AppServiceRegistry.get<Navigator>()
        navigator.navigateTo(it)
        true
    }
    private val taskRunner = MapLayerBackgroundTask()
    private val taskRunner2 = MapLayerBackgroundTask2()
    private val myLocationLayer = MyLocationLayer()
    private val tideLayer = TideMapLayer()
    private val baseMapLayer = BaseMapLayer()
    private val photoMapLayer = PhotoMapLayer()
    private var contourLayer: ContourLayer? = null
    private var hillshadeLayer: HillshadeLayer? = null
    private var elevationLayer: ElevationLayer? = null
    private val navigationLayer = NavigationLayer()
    private val scaleBarLayer = ScaleBarLayer()
    private var myElevationLayer: MyElevationLayer? = null
    private val compassLayer = CompassOverlayLayer()
    private val selectedPointLayer = ConfigurableGeoJsonLayer()
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
        hillshadeLayer = HillshadeLayer(taskRunner2)
        elevationLayer = ElevationLayer(taskRunner2)

        // Hardcoded customization for this tool
        compassLayer.backgroundColor = Resources.color(context, R.color.colorSecondary)
        compassLayer.cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
        compassLayer.paddingTopDp = 48f
        compassLayer.paddingRightDp = 8f

        distanceLayer.isEnabled = false

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
        myLocationLayer.setColor(Resources.getPrimaryMarkerColor(context))
        myLocationLayer.setAccuracyColor(Resources.getPrimaryMarkerColor(context))

        // Preferences
        scaleBarLayer.units = prefs.baseDistanceUnits
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        beaconLayer.setPreferences(prefs.map.beaconLayer)
        pathLayer.setPreferences(prefs.map.pathLayer)
        navigationLayer.setPreferences(prefs.map.navigationLayer)
        baseMapLayer.setPreferences(prefs.map.baseMapLayer)
        photoMapLayer.setPreferences(prefs.map.photoMapLayer)
        contourLayer?.setPreferences(prefs.map.contourLayer)
        elevationLayer?.setPreferences(prefs.map.elevationLayer)
        hillshadeLayer?.setPreferences(prefs.map.hillshadeLayer)
        tideLayer.setPreferences(prefs.map.tideLayer)
        myLocationLayer.setPreferences(prefs.map.myLocationLayer)
        cellTowerLayer.setPreferences(prefs.map.cellTowerLayer)

        view.setLayers(
            listOfNotNull(
                BackgroundColorMapLayer(Color.rgb(127, 127, 127)),
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
                if (prefs.map.myLocationLayer.isEnabled.get()) MyLocationLayerManager(
                    myLocationLayer
                ) else null
            )
        )

        view.start()
        layerManager?.start()

        if (view is View) {
            view.invalidate()
        }

        key++
    }

    fun pause(context: Context, view: IMapView) {
        layerManager?.stop()
        layerManager = null
        view.stop()
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
        if (location == null) {
            selectedPointLayer.setData(GeoJsonFeatureCollection(emptyList()))
        } else {
            val point = GeoJsonFeature.point(
                location,
                strokeColor = Color.WHITE,
                color = Color.BLACK
            )
            selectedPointLayer.setData(point)
        }
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
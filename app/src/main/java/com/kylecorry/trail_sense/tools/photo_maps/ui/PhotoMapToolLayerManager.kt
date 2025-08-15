package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.CompassOverlayLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ScaleBarLayer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.map.ui.BackgroundColorMapLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.BeaconLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyAccuracyLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.BeaconLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.ILayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MultiLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.NavigationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PathLayerManager
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerManager

class PhotoMapToolLayerManager {

    private var onBeaconClick: ((Beacon) -> Unit)? = null

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer {
        onBeaconClick?.invoke(it)
        true
    }
    private val myLocationLayer = MyLocationLayer()
    private val myAccuracyLayer = MyAccuracyLayer()
    private val tideLayer = TideMapLayer()
    private val contourLayer = ContourLayer()
    private val navigationLayer = NavigationLayer()
    private val scaleBarLayer = ScaleBarLayer()
    private val backgroundLayer = BackgroundColorMapLayer()
    private var myElevationLayer: MyElevationLayer? = null
    private val compassLayer = CompassOverlayLayer()
    private val selectedPointLayer = BeaconLayer()
    private val distanceLayer = MapDistanceLayer { onDistancePathChange(it) }

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()
    private var layerManager: ILayerManager? = null
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null

    fun resume(context: Context, view: IMapView) {
        val hasCompass = SensorService(context).hasCompass()

        compassLayer.backgroundColor = Resources.color(context, R.color.colorSecondary)
        compassLayer.cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
        compassLayer.paddingTopDp = 8f
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
        backgroundLayer.color = Color.WHITE

        beaconLayer.setOutlineColor(Resources.color(context, R.color.colorSecondary))

        selectedPointLayer.setOutlineColor(Color.WHITE)

        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)

        contourLayer.setPreferences(prefs.photoMaps.contourLayer)

        distanceLayer.isEnabled = false
        distanceLayer.setOutlineColor(Color.WHITE)
        distanceLayer.setPathColor(Color.BLACK)

        tideLayer.setPreferences(prefs.photoMaps.tideLayer)

        view.setLayers(
            listOfNotNull(
                if (prefs.photoMaps.contourLayer.isEnabled.get()) contourLayer else null,
                navigationLayer,
                pathLayer,
                myAccuracyLayer,
                myLocationLayer,
                if (prefs.photoMaps.tideLayer.isEnabled.get()) tideLayer else null,
                beaconLayer,
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
                PathLayerManager(context, pathLayer),
                MyAccuracyLayerManager(
                    myAccuracyLayer,
                    Resources.getPrimaryMarkerColor(context)
                ),
                MyLocationLayerManager(
                    myLocationLayer,
                    Resources.getPrimaryMarkerColor(context)
                ),
                if (prefs.photoMaps.tideLayer.isEnabled.get()) TideMapLayerManager(
                    context,
                    tideLayer
                ) else null,
                BeaconLayerManager(context, beaconLayer),
                NavigationLayerManager(context, navigationLayer)
            )
        )

        if (prefs.navigation.useRadarCompass) {
            layerManager?.start()
        }
    }

    fun pause(context: Context, view: IMapView) {
        layerManager?.stop()
        layerManager = null
    }

    fun onBearingChanged(bearing: Float) {
        layerManager?.onBearingChanged(bearing)
    }

    fun onLocationChanged(location: Coordinate, accuracy: Float?) {
        layerManager?.onLocationChanged(location, accuracy)
    }

    fun onBoundsChanged(bounds: CoordinateBounds) {
        layerManager?.onBoundsChanged(bounds)
        distanceLayer.invalidate()
    }

    fun onElevationChanged(elevation: Float) {
        myElevationLayer?.elevation = Distance.meters(elevation).convertTo(prefs.baseDistanceUnits)
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

    fun setOnBeaconClickListener(listener: ((Beacon) -> Unit)?) {
        onBeaconClick = listener
    }

}
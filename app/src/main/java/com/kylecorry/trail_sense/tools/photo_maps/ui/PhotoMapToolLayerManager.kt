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
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyAccuracyLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyElevationLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MultiLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayerManager
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayerManager
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
    private var myElevationLayer: MyElevationLayer? = null
    private val compassLayer = CompassOverlayLayer()
    private val selectedPointLayer = BeaconLayer()
    private val distanceLayer = MapDistanceLayer { onDistancePathChange(it) }

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()
    private var layerManager: ILayerManager? = null
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null

    fun resume(context: Context, view: IMapView) {
        // Location layer
        val hasCompass = SensorService(context).hasCompass()
        if (!hasCompass) {
            myLocationLayer.setShowDirection(false)
        }

        // Compass layer
        compassLayer.backgroundColor = Resources.color(context, R.color.colorSecondary)
        compassLayer.cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
        compassLayer.paddingTopDp = 8f
        compassLayer.paddingRightDp = 8f

        // Elevation layer
        myElevationLayer = MyElevationLayer(
            formatter,
            PixelCoordinate(
                Resources.dp(context, 16f),
                -Resources.dp(context, 16f)
            )
        )

        // Scale bar layer
        scaleBarLayer.units = prefs.baseDistanceUnits

        // Beacon layer
        beaconLayer.setOutlineColor(Resources.color(context, R.color.colorSecondary))
        beaconLayer.setPreferences(prefs.photoMaps.beaconLayer)

        // Selected point layer
        selectedPointLayer.setOutlineColor(Color.WHITE)

        // Path layer
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)

        // Contour layer
        contourLayer.setPreferences(prefs.photoMaps.contourLayer)

        // Distance layer
        distanceLayer.isEnabled = false
        distanceLayer.setOutlineColor(Color.WHITE)
        distanceLayer.setPathColor(Color.BLACK)

        // Tide layer
        tideLayer.setPreferences(prefs.photoMaps.tideLayer)

        // Start
        view.setLayers(
            listOfNotNull(
                if (prefs.photoMaps.contourLayer.isEnabled.get()) contourLayer else null,
                navigationLayer,
                pathLayer,
                myAccuracyLayer,
                myLocationLayer,
                if (prefs.photoMaps.tideLayer.isEnabled.get()) tideLayer else null,
                if (prefs.photoMaps.beaconLayer.isEnabled.get()) beaconLayer else null,
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
                if (prefs.photoMaps.beaconLayer.isEnabled.get()) BeaconLayerManager(
                    context,
                    beaconLayer
                ) else null,
                NavigationLayerManager(context, navigationLayer)
            )
        )

        layerManager?.start()
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

    fun setOnBeaconClickListener(listener: ((Beacon) -> Unit)?) {
        onBeaconClick = listener
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
package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
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
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.map.map_layers.BackgroundColorMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationLayer
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapLayer
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayer
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayer

class PhotoMapToolLayerManager {

    private var onBeaconClick: ((Beacon) -> Unit)? = null

    private val pathLayer = PathLayer()
    private val beaconLayer = BeaconLayer {
        onBeaconClick?.invoke(it)
        true
    }
    private val myLocationLayer = MyLocationLayer()
    private val tideLayer = TideMapLayer()
    private var contourLayer: ContourLayer? = null
    private val navigationLayer = NavigationLayer()
    private val scaleBarLayer = ScaleBarLayer()
    private var myElevationLayer: MyElevationLayer? = null
    private val compassLayer = CompassOverlayLayer()
    private var photoMapLayer: PhotoMapLayer? = null
    private val selectedPointLayer = ConfigurableGeoJsonLayer()
    private val distanceLayer = MapDistanceLayer { onDistancePathChange(it) }
    private val cellTowerLayer = CellTowerMapLayer {
        CellTowerMapLayer.navigate(it)
        true
    }

    private val prefs = AppServiceRegistry.get<UserPreferences>()
    private val formatter = AppServiceRegistry.get<FormatService>()
    private val backgroundLayer = BackgroundColorMapLayer()
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null

    private var lastMapDetails: Pair<CoordinateBounds, Double>? = null

    fun resume(context: Context, view: IMapView, photoMapId: Long) {
        contourLayer = ContourLayer()
        photoMapLayer = PhotoMapLayer(photoMapId)

        // Hardcoded customization for this tool
        myLocationLayer.setColor(Resources.getPrimaryMarkerColor(context))
        myLocationLayer.setAccuracyColor(Resources.getPrimaryMarkerColor(context))
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
        distanceLayer.isEnabled = false
        backgroundLayer.color = Resources.color(context, R.color.colorSecondary)
        lastMapDetails?.let { improveResolution(it.first, it.second) }

        // Preferences
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        pathLayer.setPreferences(prefs.photoMaps.pathLayer)
        beaconLayer.setPreferences(prefs.photoMaps.beaconLayer)
        navigationLayer.setPreferences(prefs.photoMaps.navigationLayer)
        contourLayer?.setPreferences(prefs.photoMaps.contourLayer)
        tideLayer.setPreferences(prefs.photoMaps.tideLayer)
        myLocationLayer.setPreferences(prefs.photoMaps.myLocationLayer)
        cellTowerLayer.setPreferences(prefs.photoMaps.cellTowerLayer)

        // Start
        view.setLayers(
            listOfNotNull(
                backgroundLayer,
                photoMapLayer,
                if (prefs.photoMaps.contourLayer.isEnabled.get()) contourLayer else null,
                if (prefs.photoMaps.navigationLayer.isEnabled.get()) navigationLayer else null,
                if (prefs.photoMaps.cellTowerLayer.isEnabled.get()) cellTowerLayer else null,
                if (prefs.photoMaps.pathLayer.isEnabled.get()) pathLayer else null,
                if (prefs.photoMaps.myLocationLayer.isEnabled.get()) myLocationLayer else null,
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

        view.start()
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

    fun onBoundsChanged() {
        distanceLayer.invalidate()
    }

    fun improveResolution(bounds: CoordinateBounds, metersPerPixel: Double) {
        lastMapDetails = bounds to metersPerPixel
        photoMapLayer?.improveResolution(bounds, metersPerPixel, 70)
    }

    fun onElevationChanged(elevation: Float) {
        myElevationLayer?.elevation = Distance.meters(elevation).convertTo(prefs.baseDistanceUnits)
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
            selectedPointLayer.setData(GeoJsonFeatureCollection(listOf(point)))
        }
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
package com.kylecorry.trail_sense.tools.photo_maps.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.torch.ScreenTorch
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentPhotoMapsViewBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dem.ContourLayer
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.photo_maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.BeaconLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.ILayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MultiLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyAccuracyLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.MyLocationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.NavigationLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.PathLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.layers.TideLayerManager
import com.kylecorry.trail_sense.tools.photo_maps.ui.commands.CreatePathCommand
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.layers.BeaconLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyAccuracyLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.PathLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.TideLayer
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViewPhotoMapFragment : BoundFragment<FragmentPhotoMapsViewBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val hasCompass by lazy { sensorService.hasCompass() }
    private val beaconService by lazy { BeaconService(requireContext()) }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val navigator by lazy { Navigator.getInstance(requireContext()) }
    private val screenLock by lazy { NavigationScreenLock(prefs.photoMaps.keepScreenUnlockedWhileOpen) }

    private val screenLight by lazy { ScreenTorch(requireActivity().window) }

    // Map layers
    private val tideLayer = TideLayer()
    private val beaconLayer = BeaconLayer {
        if (mapLockMode != MapLockMode.Trace) {
            navigateTo(it)
        } else {
            true
        }
    }
    private val pathLayer = PathLayer()
    private val contourLayer = ContourLayer()
    private val distanceLayer = MapDistanceLayer { onDistancePathChange(it) }
    private val myLocationLayer = MyLocationLayer()
    private val myAccuracyLayer = MyAccuracyLayer()
    private val navigationLayer = NavigationLayer()
    private val selectedPointLayer = BeaconLayer()
    private val myElevationLayer by lazy {
        MyElevationLayer(
            formatService,
            PixelCoordinate(
                Resources.dp(requireContext(), 16f),
                -Resources.dp(requireContext(), 16f)
            )
        )
    }
    private var layerManager: ILayerManager? = null

    // Paths
    private val pathService by lazy { PathService.getInstance(requireContext()) }

    private var lastDistanceToast: Toast? = null

    private var mapId = 0L
    private var map: PhotoMap? = null
    private var destination: Beacon? = null

    private var mapLockMode = MapLockMode.Free

    private val throttle = Throttle(20)

    private var shouldLockOnMapLoad = false

    // State
    private var elevation by state(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathLayer.setShouldRenderWithDrawLines(prefs.navigation.useFastPathRendering)
        mapId = requireArguments().getLong("mapId")
        shouldLockOnMapLoad = requireArguments().getBoolean("autoLockLocation", false)
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPhotoMapsViewBinding {
        return FragmentPhotoMapsViewBinding.inflate(layoutInflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        layerManager = MultiLayerManager(
            listOf(
                PathLayerManager(requireContext(), pathLayer),
                MyAccuracyLayerManager(
                    myAccuracyLayer,
                    Resources.getPrimaryMarkerColor(requireContext())
                ),
                MyLocationLayerManager(
                    myLocationLayer,
                    Resources.getPrimaryMarkerColor(requireContext())
                ),
                TideLayerManager(requireContext(), tideLayer),
                BeaconLayerManager(requireContext(), beaconLayer),
                NavigationLayerManager(requireContext(), navigationLayer),
                // selectedPointLayer and distanceLayer do not need to be managed
            )
        )
        layerManager?.start()

        // Populate the last known location and map bounds
        layerManager?.onBoundsChanged(map?.boundary())
        layerManager?.onLocationChanged(gps.location, gps.horizontalAccuracy)

        if (mapLockMode == MapLockMode.Trace) {
            updateMapLockMode(MapLockMode.Trace, prefs.photoMaps.keepMapFacingUp)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.map.setLayers(
            listOfNotNull(
                if (prefs.photoMaps.contourLayer.isEnabled) contourLayer else null,
                navigationLayer,
                pathLayer,
                myAccuracyLayer,
                myLocationLayer,
                tideLayer,
                beaconLayer,
                selectedPointLayer,
                distanceLayer,
                myElevationLayer
            )
        )
        contourLayer.setPreferences(prefs.photoMaps.contourLayer)
        distanceLayer.setOutlineColor(Color.WHITE)
        distanceLayer.setPathColor(Color.BLACK)
        distanceLayer.isEnabled = false
        selectedPointLayer.setOutlineColor(Color.WHITE)

        observe(gps) {
            layerManager?.onLocationChanged(gps.location, gps.horizontalAccuracy)
            updateDestination()

            if (mapLockMode == MapLockMode.Location || mapLockMode == MapLockMode.Compass) {
                binding.map.mapCenter = gps.location
            }
        }
        observe(altimeter) { updateDestination() }
        observe(compass) {
            compass.declination = Geology.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.rawBearing
            binding.map.azimuth = bearing
            layerManager?.onBearingChanged(bearing)
            if (mapLockMode == MapLockMode.Compass) {
                binding.map.mapAzimuth = bearing
            }
            updateDestination()
        }

        reloadMap()

        binding.map.onMapLongClick = {
            onLongPress(it)
        }

        val keepMapUp = prefs.photoMaps.keepMapFacingUp

        // TODO: Don't show if location not on map

        // Update initial map rotation
        binding.map.mapAzimuth = 0f
        binding.map.keepMapUp = keepMapUp

        // Set the button states
        CustomUiUtils.setButtonState(binding.lockBtn, false)
        CustomUiUtils.setButtonState(binding.zoomInBtn, false)
        CustomUiUtils.setButtonState(binding.zoomOutBtn, false)

        // Handle when the lock button is pressed
        binding.lockBtn.setOnClickListener {
            updateMapLockMode(getNextLockMode(mapLockMode), keepMapUp)
        }

        binding.cancelNavigationBtn.setOnClickListener {
            cancelNavigation()
        }

        binding.zoomOutBtn.setOnClickListener {
            binding.map.zoomBy(0.5f)
        }

        binding.zoomInBtn.setOnClickListener {
            binding.map.zoomBy(2f)
        }

        // Update navigation
        inBackground {
            navigator.getDestination()?.let {
                onMain {
                    navigateTo(it)
                }
            }
        }

        if (!hasCompass) {
            myLocationLayer.setShowDirection(false)
        }
    }

    private fun onLongPress(location: Coordinate) {
        if (map?.isCalibrated != true || distanceLayer.isEnabled || mapLockMode == MapLockMode.Trace) {
            return
        }

        selectLocation(location)

        lastDistanceToast?.cancel()
        Share.actions(
            this,
            formatService.formatLocation(location),
            listOf(
                ActionItem(getString(R.string.beacon), R.drawable.ic_location) {
                    createBeacon(location)
                    selectLocation(null)
                },
                ActionItem(getString(R.string.navigate), R.drawable.ic_beacon) {
                    navigateTo(location)
                    selectLocation(null)
                },
                ActionItem(getString(R.string.distance), R.drawable.ruler) {
                    startDistanceMeasurement(gps.location, location)
                    selectLocation(null)
                },
            )
        ) {
            selectLocation(null)
        }
    }

    fun reloadMap() {
        inBackground {
            withContext(Dispatchers.IO) {
                map = mapRepo.getMap(mapId)
            }
            withContext(Dispatchers.Main) {
                map?.let {
                    onMapLoad(it)
                }
            }
        }
    }

    private fun updateDestination() {
        if (throttle.isThrottled()) {
            return
        }

        elevation = altimeter.altitude

        val beacon = destination ?: return
        binding.navigationSheet.show(
            gps.location,
            altimeter.altitude,
            gps.speed.speed,
            beacon,
            compass.declination,
            true
        )
    }

    private fun selectLocation(location: Coordinate?) {
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

    private fun createBeacon(location: Coordinate) {
        val bundle = bundleOf(
            "initial_location" to GeoUri(location)
        )
        findNavController().navigate(R.id.placeBeaconFragment, bundle)
    }

    private fun showDistance(distance: Distance) {
        lastDistanceToast?.cancel()
        val relative = distance
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        binding.distanceSheet.setDistance(relative)
    }

    fun startDistanceMeasurement(vararg initialPoints: Coordinate) {
        if (map?.isCalibrated != true) {
            toast(getString(R.string.map_is_not_calibrated))
            return
        }

        distanceLayer.isEnabled = true
        distanceLayer.clear()
        initialPoints.forEach { distanceLayer.add(it) }
        binding.distanceSheet.show()
        binding.distanceSheet.cancelListener = {
            stopDistanceMeasurement()
        }
        binding.distanceSheet.createPathListener = {
            inBackground {
                map?.let {
                    val id = CreatePathCommand(
                        pathService,
                        prefs.navigation,
                        it
                    ).execute(distanceLayer.getPoints())

                    onMain {
                        findNavController().navigate(
                            R.id.action_maps_to_path,
                            bundleOf("path_id" to id)
                        )
                    }
                }
            }
        }
        binding.distanceSheet.undoListener = {
            distanceLayer.undo()
        }
    }

    private fun stopDistanceMeasurement() {
        distanceLayer.isEnabled = false
        distanceLayer.clear()
        binding.distanceSheet.hide()
    }

    fun trace() {
        updateMapLockMode(MapLockMode.Trace, prefs.photoMaps.keepMapFacingUp)
    }

    private fun navigateTo(location: Coordinate) {
        inBackground {
            // Create a temporary beacon
            val beacon = Beacon(
                0L,
                map?.name ?: "",
                location,
                visible = false,
                temporary = true,
                color = AppColor.Orange.color,
                owner = BeaconOwner.Maps
            )
            val id = onIO {
                beaconService.add(beacon)
            }

            navigateTo(beacon.copy(id = id))
        }
    }

    private fun onDistancePathChange(points: List<Coordinate>) {
        // Display distance
        val distance = Geology.getPathDistance(points)
        showDistance(distance)
    }

    private fun navigateTo(beacon: Beacon): Boolean {
        navigator.navigateTo(beacon)
        destination = beacon
        binding.cancelNavigationBtn.show()
        updateDestination()
        activity?.let { screenLock.updateLock(it) }
        return true
    }

    private fun hideNavigation() {
        binding.cancelNavigationBtn.hide()
        binding.navigationSheet.hide()
        activity?.let { screenLock.updateLock(it) }
        destination = null
    }

    private fun cancelNavigation() {
        if (mapLockMode == MapLockMode.Trace) {
            return
        }

        navigator.cancelNavigation()
        destination = null
        hideNavigation()
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.map.onImageLoadedListener = {
            if (shouldLockOnMapLoad) {
                updateMapLockMode(MapLockMode.Location, prefs.photoMaps.keepMapFacingUp)
                shouldLockOnMapLoad = false
            }
        }
        binding.map.showMap(map)
        layerManager?.onBoundsChanged(map.boundary())
    }

    private fun updateMapLockMode(mode: MapLockMode, keepMapUp: Boolean) {
        mapLockMode = mode

        // Show zoom buttons
        binding.zoomInBtn.isVisible = true
        binding.zoomOutBtn.isVisible = true
        binding.map.isZoomEnabled = true

        // Show the bottom navigation
        requireMainActivity().setBottomNavigationEnabled(true)

        when (mapLockMode) {
            MapLockMode.Location -> {
                // Disable pan
                binding.map.isPanEnabled = false

                // Zoom in and center on location
                binding.map.metersPerPixel = 0.5f
                binding.map.mapCenter = gps.location

                // Reset the rotation
                binding.map.mapAzimuth = 0f
                binding.map.keepMapUp = keepMapUp

                // Show as locked
                binding.lockBtn.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(binding.lockBtn, true)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Compass -> {
                // Disable pan
                binding.map.isPanEnabled = false

                // Center on location
                binding.map.mapCenter = gps.location

                // Rotate
                binding.map.keepMapUp = false
                binding.map.mapAzimuth = -compass.rawBearing

                // Show as locked
                binding.lockBtn.setImageResource(R.drawable.ic_compass_icon)
                CustomUiUtils.setButtonState(binding.lockBtn, true)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Free -> {
                // Enable pan
                binding.map.isPanEnabled = true

                // Reset the rotation
                binding.map.mapAzimuth = 0f
                binding.map.keepMapUp = keepMapUp

                // Show as unlocked
                binding.lockBtn.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(binding.lockBtn, false)

                // Reset brightness
                screenLight.off()
            }

            MapLockMode.Trace -> {
                CustomUiUtils.disclaimer(
                    requireContext(),
                    getString(R.string.trace),
                    getString(R.string.map_trace_instructions),
                    "disclaimer_shown_map_trace",
                    cancelText = null
                )

                // Disable pan
                binding.map.setPanEnabled(false, false)
                binding.map.isZoomEnabled = false

                // Show as locked
                binding.lockBtn.setImageResource(R.drawable.lock)
                CustomUiUtils.setButtonState(binding.lockBtn, true)

                // Full brightness
                screenLight.on()

                // Hide zoom buttons
                binding.zoomInBtn.isVisible = false
                binding.zoomOutBtn.isVisible = false

                // Hide the bottom navigation
                requireMainActivity().setBottomNavigationEnabled(false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        layerManager?.stop()
        layerManager = null
        lastDistanceToast?.cancel()

        // Reset brightness
        screenLight.off()

        // Show the bottom navigation
        requireMainActivity().setBottomNavigationEnabled(true)
    }

    fun recenter() {
        if (mapLockMode == MapLockMode.Trace) {
            return
        }
        binding.map.recenter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.let { screenLock.releaseLock(it) }
    }

    override fun onUpdate() {
        super.onUpdate()

        effect("elevation", elevation, lifecycleHookTrigger.onResume()) {
            myElevationLayer.elevation =
                Distance.meters(elevation).convertTo(prefs.baseDistanceUnits)
        }

        useEffect(resetOnResume) {
            activity?.let { screenLock.updateLock(it) }
        }
    }

    private fun getNextLockMode(mode: MapLockMode): MapLockMode {
        return when (mode) {
            MapLockMode.Location -> {
                if (hasCompass) {
                    MapLockMode.Compass
                } else {
                    MapLockMode.Free
                }
            }

            MapLockMode.Compass -> {
                MapLockMode.Free
            }

            MapLockMode.Free -> {
                MapLockMode.Location
            }

            MapLockMode.Trace -> {
                MapLockMode.Free
            }
        }
    }

    private enum class MapLockMode {
        Location,
        Compass,
        Free,
        Trace
    }

    companion object {
        fun create(mapId: Long, autoLockLocation: Boolean = false): ViewPhotoMapFragment {
            return ViewPhotoMapFragment().apply {
                arguments = bundleOf("mapId" to mapId, "autoLockLocation" to autoLockLocation)
            }
        }
    }

}
package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsViewBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.ui.asMappable
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.navigation.ui.layers.*
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.domain.PhotoMap
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class ViewMapFragment : BoundFragment<FragmentMapsViewBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val beaconService by lazy { BeaconService(requireContext()) }
    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private var pathPoints: Map<Long, List<PathPoint>> = emptyMap()
    private var paths: List<Path> = emptyList()
    private var currentBacktrackPathId: Long? = null
    private val cache by lazy { Preferences(requireContext()) }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    // Map layers
    private val tideLayer = TideLayer()
    private val beaconLayer = BeaconLayer { navigateTo(it) }
    private val pathLayer = PathLayer()
    private val distanceLayer = MapDistanceLayer { onDistancePathChange(it) }
    private val myLocationLayer = MyLocationLayer()
    private val navigationLayer = NavigationLayer()
    private val selectedPointLayer = BeaconLayer()

    private var lastDistanceToast: Toast? = null

    private var mapId = 0L
    private var map: PhotoMap? = null
    private var destination: Beacon? = null

    private var calibrationPoint1Percent: PercentCoordinate? = null
    private var calibrationPoint2Percent: PercentCoordinate? = null
    private var calibrationPoint1: Coordinate? = null
    private var calibrationPoint2: Coordinate? = null
    private var calibrationIndex = 0
    private var isCalibrating = false

    private var locationLocked = false
    private var compassLocked = false

    private val throttle = Throttle(20)

    private var beacons: List<Beacon> = emptyList()

    private val tideTimer = Timer {
        updateTides()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapsViewBinding {
        return FragmentMapsViewBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.map.setLayers(
            listOf(
                navigationLayer,
                pathLayer,
                myLocationLayer,
                tideLayer,
                beaconLayer,
                selectedPointLayer,
                distanceLayer
            )
        )
        distanceLayer.setOutlineColor(Color.WHITE)
        distanceLayer.setPathColor(Color.BLACK)
        distanceLayer.isEnabled = false
        beaconLayer.setOutlineColor(Color.WHITE)
        selectedPointLayer.setOutlineColor(Color.WHITE)
        myLocationLayer.setColor(AppColor.Orange.color)

        observe(gps) {
            myLocationLayer.setLocation(gps.location)
            binding.map.setMyLocation(gps.location)
            navigationLayer.setStart(gps.location)
            displayPaths()
            updateDestination()
            if (!tideTimer.isRunning()) {
                tideTimer.interval(Duration.ofMinutes(1))
            }
            if (locationLocked) {
                binding.map.mapCenter = gps.location
            }
        }
        observe(altimeter) { updateDestination() }
        observe(compass) {
            compass.declination = Geology.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.bearing
            binding.map.azimuth = bearing
            myLocationLayer.setAzimuth(bearing)
            if (compassLocked) {
                myLocationLayer.setAzimuth(Bearing(0f)) // TODO: Not sure why this is needed - it shouldn't be
                binding.map.mapRotation = bearing.value
            }
            updateDestination()
        }
        observe(beaconRepo.getBeacons()) {
            beacons = it.map { it.toBeacon() }.filter { it.visible }
            updateBeacons()
        }

        observe(pathService.getLivePaths()) {
            paths = it.filter { path -> path.style.visible }
            inBackground {
                withContext(Dispatchers.IO) {
                    currentBacktrackPathId = pathService.getBacktrackPathId()
                    pathPoints = pathService.getWaypoints(paths.map { path -> path.id })
                        .mapValues { it.value.sortedByDescending { it.id } }
                }
                withContext(Dispatchers.Main) {
                    displayPaths()
                }
            }
        }

        reloadMap()

        binding.calibrationNext.setOnClickListener {
            if (calibrationIndex == 1) {
                isCalibrating = false
                showZoomBtns()
                if (destination != null) {
                    navigateTo(destination!!)
                }
                binding.mapCalibrationBottomPanel.isVisible = false
                binding.map.hideCalibrationPoints()
                inBackground {
                    withContext(Dispatchers.IO) {
                        map?.let {
                            var updated = mapRepo.getMap(it.id)!!
                            updated = updated.copy(
                                calibration = updated.calibration.copy(calibrationPoints = it.calibration.calibrationPoints)
                            )
                            mapRepo.addMap(updated)
                            map = updated
                        }
                    }
                }
            } else {
                calibratePoint(++calibrationIndex)
            }
        }

        binding.calibrationPrev.setOnClickListener {
            calibratePoint(--calibrationIndex)
        }


        binding.mapCalibrationCoordinate.setOnCoordinateChangeListener {
            if (isCalibrating) {
                if (calibrationIndex == 0) {
                    calibrationPoint1 = it
                } else {
                    calibrationPoint2 = it
                }

                updateMapCalibration()
                binding.map.showCalibrationPoints()
            }
        }

        binding.map.onMapClick = {
            if (isCalibrating) {
                if (calibrationIndex == 0) {
                    calibrationPoint1Percent = it
                } else {
                    calibrationPoint2Percent = it
                }
                updateMapCalibration()
                binding.map.showCalibrationPoints()
            }
        }

        binding.map.onMapLongClick = {
            onLongPress(it)
        }

        // TODO: Don't show if not calibrated or location not on map
        locationLocked = false
        compassLocked = false
        binding.map.mapRotation = 0f
        CustomUiUtils.setButtonState(binding.lockBtn, false)
        CustomUiUtils.setButtonState(binding.zoomInBtn, false)
        CustomUiUtils.setButtonState(binding.zoomOutBtn, false)
        binding.lockBtn.setOnClickListener {
            // TODO: If user drags too far from location, don't follow their location or rotate with them
            if (!locationLocked && !compassLocked) {
                binding.map.isPanEnabled = false
                binding.map.metersPerPixel = 0.5f
                binding.map.mapCenter = gps.location
                // TODO: Make this the GPS icon (locked)
                binding.lockBtn.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(binding.lockBtn, true)
                locationLocked = true
            } else if (locationLocked && !compassLocked) {
                compassLocked = true
                binding.map.mapRotation = -compass.rawBearing
                binding.lockBtn.setImageResource(R.drawable.ic_compass_icon)
                CustomUiUtils.setButtonState(binding.lockBtn, true)
            } else {
                binding.map.isPanEnabled = true
                locationLocked = false
                compassLocked = false
                binding.map.mapRotation = 0f
                // TODO: Make this the GPS icon (unlocked)
                binding.lockBtn.setImageResource(R.drawable.satellite)
                CustomUiUtils.setButtonState(binding.lockBtn, false)
            }
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

        val dest = cache.getLong(NavigatorFragment.LAST_BEACON_ID)
        if (dest != null) {
            inBackground {
                val beacon = withContext(Dispatchers.IO) {
                    beaconRepo.getBeacon(dest)?.toBeacon()
                }
                if (beacon != null) {
                    withContext(Dispatchers.Main) {
                        navigateTo(beacon)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.setMyLocation(gps.location)
    }

    private fun onLongPress(location: Coordinate) {
        if (map?.isCalibrated != true) {
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

    private fun updateBeacons() {
        val all = (beacons + listOfNotNull(destination)).distinctBy { it.id }
        beaconLayer.setBeacons(all)
    }

    private fun displayPaths() {
        val isTracking = BacktrackScheduler.isOn(requireContext())
        val mappablePaths = mutableListOf<IMappablePath>()
        val currentPathId = currentBacktrackPathId
        for (points in pathPoints) {
            val path = paths.firstOrNull { it.id == points.key } ?: continue
            val pts = if (isTracking && currentPathId == path.id) {
                listOf(gps.getPathPoint(currentPathId)) + points.value
            } else {
                points.value
            }
            mappablePaths.add(pts.asMappable(requireContext(), path))
        }

        pathLayer.setPaths(mappablePaths)
    }

    private fun updateDestination() {
        if (throttle.isThrottled() || isCalibrating) {
            return
        }

        val beacon = destination ?: return
        binding.navigationSheet.show(
            Position(gps.location, altimeter.altitude, compass.bearing, gps.speed.speed),
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
        findNavController().navigate(R.id.place_beacon, bundle)
    }

    private fun showDistance(distance: Distance) {
        lastDistanceToast?.cancel()
        val relative = distance
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        binding.distanceSheet.setDistance(relative)
    }

    private fun startDistanceMeasurement(vararg initialPoints: Coordinate) {
        distanceLayer.isEnabled = true
        distanceLayer.clear()
        initialPoints.forEach { distanceLayer.add(it) }
        binding.distanceSheet.show()
        binding.distanceSheet.cancelListener = {
            stopDistanceMeasurement()
        }
    }

    private fun stopDistanceMeasurement() {
        distanceLayer.isEnabled = false
        distanceLayer.clear()
        binding.distanceSheet.hide()
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
        if (isCalibrating) {
            return false
        }
        cache.putLong(NavigatorFragment.LAST_BEACON_ID, beacon.id)
        destination = beacon
        val colorWithAlpha = beacon.color.withAlpha(127)
        navigationLayer.setColor(colorWithAlpha)
        navigationLayer.setEnd(beacon.coordinate)
        beaconLayer.highlight(beacon)
        binding.cancelNavigationBtn.show()
        updateBeacons()
        updateDestination()
        return true
    }

    private fun hideNavigation() {
        navigationLayer.setEnd(null)
        beaconLayer.highlight(null)
        binding.cancelNavigationBtn.hide()
        binding.navigationSheet.hide()
        destination = null
        updateBeacons()
    }

    private fun hideZoomBtns() {
        binding.zoomInBtn.hide()
        binding.zoomOutBtn.hide()
        binding.lockBtn.hide()
    }

    private fun showZoomBtns() {
        binding.zoomInBtn.show()
        binding.zoomOutBtn.show()
        binding.lockBtn.show()
    }

    private fun cancelNavigation() {
        cache.remove(NavigatorFragment.LAST_BEACON_ID)
        destination = null
        hideNavigation()
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        binding.map.showMap(map)
        if (map.calibration.calibrationPoints.size < 2) {
            calibrateMap()
        }
    }

    private fun updateMapCalibration() {
        val points = mutableListOf<MapCalibrationPoint>()
        if (calibrationPoint1Percent != null) {
            points.add(
                MapCalibrationPoint(
                    calibrationPoint1 ?: Coordinate.zero,
                    calibrationPoint1Percent!!
                )
            )
        }

        if (calibrationPoint2Percent != null) {
            points.add(
                MapCalibrationPoint(
                    calibrationPoint2 ?: Coordinate.zero,
                    calibrationPoint2Percent!!
                )
            )
        }

        map = map?.copy(calibration = map!!.calibration.copy(calibrationPoints = points))
        binding.map.showMap(map!!)
    }

    override fun onPause() {
        super.onPause()
        tideTimer.stop()
        lastDistanceToast?.cancel()
    }

    fun calibrateMap() {
        map ?: return
        isCalibrating = true
        hideNavigation()
        hideZoomBtns()
        loadCalibrationPointsFromMap()

        calibrationIndex = if (calibrationPoint1 == null || calibrationPoint1Percent == null) {
            0
        } else {
            1
        }

        calibratePoint(calibrationIndex)
        binding.map.showCalibrationPoints()
    }

    fun recenter() {
        binding.map.recenter()
    }

    private fun calibratePoint(index: Int) {
        loadCalibrationPointsFromMap()
        binding.mapCalibrationTitle.text = getString(R.string.calibrate_map_point, index + 1, 2)
        binding.mapCalibrationCoordinate.coordinate =
            if (index == 0) calibrationPoint1 else calibrationPoint2
        binding.mapCalibrationBottomPanel.isVisible = true
        binding.calibrationNext.text =
            if (index == 0) getString(R.string.next) else getString(R.string.done)
        binding.calibrationPrev.isEnabled = index == 1
    }

    private fun loadCalibrationPointsFromMap() {
        map ?: return
        val first =
            if (map!!.calibration.calibrationPoints.isNotEmpty()) map!!.calibration.calibrationPoints[0] else null
        val second =
            if (map!!.calibration.calibrationPoints.size > 1) map!!.calibration.calibrationPoints[1] else null
        calibrationPoint1 = first?.location
        calibrationPoint2 = second?.location
        calibrationPoint1Percent = first?.imageLocation
        calibrationPoint2Percent = second?.imageLocation
    }

    private fun updateTides() = inBackground {
        val context = context ?: return@inBackground
        // TODO: Limit to nearby tides
        val tables = LoadAllTideTablesCommand(context).execute()
        val currentTideCommand = CurrentTideTypeCommand(TideService())
        val tides = tables.filter { it.location != null && it.isVisible }.map {
            it to currentTideCommand.execute(it)
        }
        tideLayer.setTides(tides)
    }

}
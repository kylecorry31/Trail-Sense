package com.kylecorry.trail_sense.tools.maps.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.topics.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsViewBinding
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.paths.domain.Path
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.navigation.paths.ui.asMappable
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.navigation.ui.layers.*
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Position
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorUtils.withAlpha
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.getPathPoint
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.domain.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.maps.domain.PercentCoordinate
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideTypeCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.LoadAllTideTablesCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration

class ViewMapFragment : BoundFragment<FragmentMapsViewBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val pathService by lazy { PathService.getInstance(requireContext()) }
    private var pathPoints: kotlin.collections.Map<Long, List<PathPoint>> = emptyMap()
    private var paths: List<Path> = emptyList()
    private var currentBacktrackPathId: Long? = null
    private val geoService by lazy { GeologyService() }
    private val cache by lazy { Preferences(requireContext()) }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }

    private val tideLayer = TideLayer()
    private val beaconLayer = BeaconLayer { navigateTo(it) }
    private val pathLayer = PathLayer()
    private val myLocationLayer = MyLocationLayer()
    private val navigationLayer = NavigationLayer()

    private var mapId = 0L
    private var map: Map? = null
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
                beaconLayer
            )
        )
        beaconLayer.setOutlineColor(Color.WHITE)
        myLocationLayer.setColor(AppColor.Orange.color)

        gps.asLiveData().observe(viewLifecycleOwner) {
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
        altimeter.asLiveData().observe(viewLifecycleOwner) { updateDestination() }
        compass.asLiveData().observe(viewLifecycleOwner) {
            compass.declination = geoService.getGeomagneticDeclination(gps.location, gps.altitude)
            val bearing = compass.bearing
            binding.map.azimuth = bearing
            myLocationLayer.setAzimuth(bearing)
            if (compassLocked) {
                myLocationLayer.setAzimuth(Bearing(0f)) // TODO: Not sure why this is needed - it shouldn't be
                binding.map.mapRotation = bearing.value
            }
            updateDestination()
        }
        beaconRepo.getBeacons().observe(viewLifecycleOwner) {
            beacons = it.map { it.toBeacon() }.filter { it.visible }
            updateBeacons()
        }

        pathService.getLivePaths().observe(viewLifecycleOwner) {
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
                            val updated = mapRepo.getMap(it.id)!!.copy(calibrationPoints = it.calibrationPoints)
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
            val formatted = formatService.formatLocation(it)
            // TODO: ask to create or navigate
            Alerts.dialog(
                requireContext(),
                getString(R.string.create_beacon),
                getString(R.string.place_beacon_at, formatted),
                okText = getString(R.string.beacon_create)
            ) { cancelled ->
                if (!cancelled) {
                    val bundle = bundleOf(
                        "initial_location" to GeoUri(it)
                    )
                    findNavController().navigate(R.id.place_beacon, bundle)
                }
            }
        }

        // TODO: Don't show if not calibrated or location not on map
        locationLocked = false
        compassLocked = false
        binding.map.mapRotation = 0f
        CustomUiUtils.setButtonState(binding.lockBtn, false)
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

    private fun onMapLoad(map: Map) {
        this.map = map
        binding.map.showMap(map)
        if (map.calibrationPoints.size < 2) {
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

        map = map?.copy(calibrationPoints = points)
        binding.map.showMap(map!!)
    }

    override fun onPause() {
        super.onPause()
        tideTimer.stop()
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
        val first = if (map!!.calibrationPoints.isNotEmpty()) map!!.calibrationPoints[0] else null
        val second = if (map!!.calibrationPoints.size > 1) map!!.calibrationPoints[1] else null
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
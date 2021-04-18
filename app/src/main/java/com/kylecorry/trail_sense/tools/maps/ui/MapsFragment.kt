package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentMapsBinding
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.backtrack.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.backtrack.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.geo.PathPoint
import com.kylecorry.trailsensecore.domain.geo.cartography.MapCalibrationPoint
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.Position
import com.kylecorry.trailsensecore.domain.pixels.PercentCoordinate
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Throttle
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import com.kylecorry.trailsensecore.domain.geo.cartography.Map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class MapsFragment : BoundFragment<FragmentMapsBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val backtrackRepo by lazy { WaypointRepo.getInstance(requireContext()) }
    private val geoService by lazy { GeoService() }
    private val cache by lazy { Cache(requireContext()) }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var mapId = 0L
    private var map: Map? = null
    private var destination: Beacon? = null

    private var calibrationPoint1Percent: PercentCoordinate? = null
    private var calibrationPoint2Percent: PercentCoordinate? = null
    private var calibrationPoint1: Coordinate? = null
    private var calibrationPoint2: Coordinate? = null
    private var calibrationIndex = 0
    private var isCalibrating = false

    private var backtrack: Path? = null

    private val throttle = Throttle(20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapsBinding {
        return FragmentMapsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gps.asLiveData().observe(viewLifecycleOwner, {
            binding.map.setMyLocation(gps.location)
            displayPaths()
            updateDestination()
        })
        altimeter.asLiveData().observe(viewLifecycleOwner, { updateDestination() })
        compass.asLiveData().observe(viewLifecycleOwner, {
            compass.declination = geoService.getDeclination(gps.location, gps.altitude)
            binding.map.setAzimuth(compass.bearing)
            updateDestination()
        })
        beaconRepo.getBeacons()
            .observe(
                viewLifecycleOwner,
                { binding.map.setBeacons(it.map { it.toBeacon() }.filter { it.visible }) })

        if (prefs.navigation.showBacktrackPath) {
            backtrackRepo.getWaypoints()
                .observe(viewLifecycleOwner, { waypoints ->
                    val sortedWaypoints = waypoints
                        .sortedByDescending { it.createdInstant }

                    backtrack = Path(
                        WaypointRepo.BACKTRACK_PATH_ID,
                        getString(R.string.tool_backtrack_title),
                        sortedWaypoints.map { it.toPathPoint() },
                        UiUtils.color(requireContext(), R.color.colorAccent),
                        true
                    )
                    displayPaths()
                })
        }


        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                map = mapRepo.getMap(mapId)
            }
            withContext(Dispatchers.Main) {
                map?.let {
                    onMapLoad(it)
                }
            }
        }

        binding.calibrationNext.setOnClickListener {
            if (calibrationIndex == 1) {
                isCalibrating = false
                if (destination != null) {
                    navigateTo(destination!!)
                }
                binding.mapCalibrationBottomPanel.isVisible = false
                binding.map.hideCalibrationPoints()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        map?.let {
                            mapRepo.addMap(it)
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

        binding.menuBtn.setOnClickListener {
            UiUtils.openMenu(it, R.menu.map_menu) {
                when (it) {
                    R.id.action_map_delete -> {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                map?.let {
                                    mapRepo.deleteMap(it)
                                }
                            }
                            withContext(Dispatchers.IO) {
                                requireActivity().onBackPressed()
                            }
                        }
                    }
                    R.id.action_map_guide -> {
                        UserGuideUtils.openGuide(this, R.raw.importing_maps)
                    }
                    R.id.action_map_rename -> {
                        CustomUiUtils.pickText(
                            requireContext(),
                            getString(R.string.create_map),
                            getString(R.string.create_map_description),
                            map?.name,
                            hint = getString(R.string.name_hint)
                        ) {
                            if (it != null) {
                                map = map?.copy(name = it)
                                binding.mapName.text = it
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        map?.let {
                                            mapRepo.addMap(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    R.id.action_map_calibrate -> {
                        calibrateMap()
                    }
                    else -> {
                    }
                }
                true
            }
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

        binding.map.onMapImageClick = {
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

        binding.map.onSelectLocation = {
            val formatted = formatService.formatLocation(it)
            // TODO: ask to create or navigate
            UiUtils.alertWithCancel(
                requireContext(),
                getString(R.string.create_beacon_title),
                getString(R.string.place_beacon_at, formatted),
                getString(R.string.beacon_create),
                getString(R.string.dialog_cancel)
            ) { cancelled ->
                if (!cancelled) {
                    val bundle = bundleOf(
                        "initial_location" to MyNamedCoordinate(it)
                    )
                    findNavController().navigate(R.id.place_beacon, bundle)
                }
            }
        }

        binding.map.onSelectBeacon = {
            navigateTo(it)
        }

        binding.cancelNavigationBtn.setOnClickListener {
            cancelNavigation()
        }

        val dest = cache.getLong(NavigatorFragment.LAST_BEACON_ID)
        if (dest != null) {
            lifecycleScope.launch {
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

    private fun displayPaths() {
        val myLocation = PathPoint(
            0,
            WaypointRepo.BACKTRACK_PATH_ID,
            gps.location,
            time = Instant.now()
        )

        val backtrackPath = backtrack?.copy(points = listOf(myLocation) + backtrack!!.points)

        binding.map.setPaths(listOfNotNull(backtrackPath))
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

    private fun navigateTo(beacon: Beacon) {
        cache.putLong(NavigatorFragment.LAST_BEACON_ID, beacon.id)
        destination = beacon
        if (!isCalibrating) {
            binding.map.setDestination(beacon)
            binding.cancelNavigationBtn.show()
            updateDestination()
        }
    }

    private fun hideNavigation() {
        binding.map.setDestination(null)
        binding.cancelNavigationBtn.hide()
        binding.navigationSheet.hide()
    }

    private fun cancelNavigation() {
        cache.remove(NavigatorFragment.LAST_BEACON_ID)
        destination = null
        hideNavigation()
    }

    private fun onMapLoad(map: Map) {
        this.map = map
        binding.mapName.text = map.name
        binding.map.setMap(map)
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
        binding.map.setMap(map!!, false)
    }

    private fun calibrateMap() {
        map ?: return
        isCalibrating = true
        hideNavigation()
        loadCalibrationPointsFromMap()

        calibrationIndex = if (calibrationPoint1 == null || calibrationPoint1Percent == null) {
            0
        } else {
            1
        }

        calibratePoint(calibrationIndex)
        binding.map.showCalibrationPoints()
    }

    private fun calibratePoint(index: Int) {
        loadCalibrationPointsFromMap()
        binding.mapCalibrationTitle.text = "Calibrate point ${index + 1}"
        binding.mapCalibrationCoordinate.coordinate =
            if (index == 0) calibrationPoint1 else calibrationPoint2
        binding.mapCalibrationBottomPanel.isVisible = true
        binding.calibrationNext.text = if (index == 0) "Next" else "Done"
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

}
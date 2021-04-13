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
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trailsensecore.domain.geo.cartography.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.MapRepo
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.geo.cartography.MapCalibrationPoint
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.pixels.PercentCoordinate
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsFragment : BoundFragment<FragmentMapsBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val geoService by lazy { GeoService() }
    private val cache by lazy { Cache(requireContext()) }
    private val mapRepo by lazy { MapRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private var mapId = 0L
    private var map: Map? = null
    private var destination: Beacon? = null

    private var calibrationPoint1Percent: PercentCoordinate? = null
    private var calibrationPoint2Percent: PercentCoordinate? = null
    private var calibrationPoint1: Coordinate? = null
    private var calibrationPoint2: Coordinate? = null
    private var calibrationIndex = 0
    private var isCalibrating = false

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
        gps.asLiveData().observe(viewLifecycleOwner, { binding.map.setMyLocation(gps.location) })
        compass.asLiveData().observe(viewLifecycleOwner, {
            compass.declination = geoService.getDeclination(gps.location, gps.altitude)
            binding.map.setAzimuth(compass.bearing)
        })
        beaconRepo.getBeacons()
            .observe(viewLifecycleOwner, { binding.map.setBeacons(it.map { it.toBeacon() }.filter { it.visible }) })


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
            CustomUiUtils.openMenu(it, R.menu.map_menu) {
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
            cache.putLong(NavigatorFragment.LAST_BEACON_ID, it.id)
            destination = it
            binding.map.setDestination(it)
        }

        val dest = cache.getLong(NavigatorFragment.LAST_BEACON_ID)
        if (dest != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    destination = beaconRepo.getBeacon(dest)?.toBeacon()
                }
                withContext(Dispatchers.Main) {
                    binding.map.setDestination(destination)
                }
            }
        }
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
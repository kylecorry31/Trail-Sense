package com.kylecorry.trail_sense.tools.maps.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.databinding.FragmentMapsBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.navigation.ui.NavigatorFragment
import com.kylecorry.trail_sense.shared.BoundFragment
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsFragment: BoundFragment<FragmentMapsBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }
    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val geoService by lazy { GeoService() }
    private val cache by lazy { Cache(requireContext()) }

    private var destination: Beacon? = null

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
        beaconRepo.getBeacons().observe(viewLifecycleOwner, { binding.map.setBeacons(it.map { it.toBeacon() }) })
        binding.map.setCalibrationPoints(
            // TODO: Switch to percentages rather than pixels, so the user can use the map on other devices (ex. if export ever happens)
            MapCalibrationPoint(Coordinate.parse("19T 0315000E 4910000N", CoordinateFormat.UTM)!!, PixelCoordinate(100.35407f, 209.07849f)),
            MapCalibrationPoint(Coordinate.parse("19T 0321000E 4903000N", CoordinateFormat.UTM)!!, PixelCoordinate(904.0879f, 1078.4873f))
        )
        val dest = cache.getLong(NavigatorFragment.LAST_BEACON_ID)
        if (dest != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    destination = beaconRepo.getBeacon(dest)?.toBeacon()
                }
                withContext(Dispatchers.Main){
                    binding.map.setDestination(destination)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
    }
}
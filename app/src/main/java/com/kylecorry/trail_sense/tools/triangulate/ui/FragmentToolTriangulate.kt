package com.kylecorry.trail_sense.tools.triangulate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.units.Bearing
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolTriangulateBinding
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.NavigationService

class FragmentToolTriangulate : BoundFragment<FragmentToolTriangulateBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensorService.getCompass() }
    private val geoService = GeoService()
    private val navigationService = NavigationService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var direction1: Bearing? = null
    private var direction2: Bearing? = null
    private var location: Coordinate? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bearingTo1Btn.setOnClickListener {
            direction1 = compass.bearing
            binding.bearingTo1.text = formatService.formatDegrees(compass.bearing.value, replace360 = true)
            update()
        }
        binding.bearingTo2Btn.setOnClickListener {
            direction2 = compass.bearing
            binding.bearingTo2.text = formatService.formatDegrees(compass.bearing.value, replace360 = true)
            update()
        }

        binding.copyLocation.setOnClickListener {
            location?.let {
                Clipboard.copy(
                    requireContext(),
                    formatService.formatLocation(it),
                    getString(R.string.copied_to_clipboard_toast)
                )
            }
        }

        binding.placeBeaconBtn.setOnClickListener {
            location?.let {
                AppUtils.placeBeacon(requireContext(), MyNamedCoordinate(it))
            }
        }

        binding.gpsOverrideBtn.setOnClickListener {
            location?.let { coord ->
                prefs.locationOverride = coord
                Alerts.toast(requireContext(), getString(R.string.location_override_updated))
            }
        }

        binding.triangulate1.setOnCoordinateChangeListener { update() }
        binding.triangulate2.setOnCoordinateChangeListener { update() }

    }


    override fun onResume() {
        super.onResume()
        compass.start(this::compassUpdate)
        if (prefs.useAutoLocation) {
            binding.gpsOverrideBtn.visibility = View.INVISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        compass.stop(this::compassUpdate)
    }

    private fun compassUpdate(): Boolean {
        binding.bearingTo1Btn.text = getString(R.string.beacon_set_bearing_btn, formatService.formatDegrees(compass.bearing.value, replace360 = true))
        binding.bearingTo2Btn.text = getString(R.string.beacon_set_bearing_btn, formatService.formatDegrees(compass.bearing.value, replace360 = true))
        return true
    }

    private fun update() {
        if (!isBound){
            return
        }
        val c1 = binding.triangulate1.coordinate ?: return
        val c2 = binding.triangulate2.coordinate ?: return
        val d1 = direction1 ?: return
        val d2 = direction2 ?: return

        // All information is available to triangulate
        val declination = geoService.getDeclination(c1)
        val bearing1 = d1.withDeclination(declination)
        val bearing2 = d2.withDeclination(declination)

        val location = navigationService.triangulate(c1, bearing1, c2, bearing2)
        this.location = location

        if (location == null || location.latitude.isNaN() || location.longitude.isNaN()) {
            binding.location.text = getString(R.string.could_not_triangulate)
            binding.copyLocation.visibility = View.INVISIBLE
            binding.gpsOverrideBtn.isVisible = false
            binding.placeBeaconBtn.isVisible = false
        } else {
            binding.location.text = formatService.formatLocation(location)
            binding.copyLocation.visibility = View.VISIBLE
            binding.placeBeaconBtn.isVisible = true
            if (!prefs.useAutoLocation) {
                binding.gpsOverrideBtn.isVisible = true
            }
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolTriangulateBinding {
        return FragmentToolTriangulateBinding.inflate(layoutInflater, container, false)
    }


}
package com.kylecorry.trail_sense.tools.triangulate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolTriangulateBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.NavigationService
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils

class FragmentToolTriangulate : Fragment() {

    private var _binding: FragmentToolTriangulateBinding? = null
    private val binding get() = _binding!!

    private val sensorService by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensorService.getCompass() }
    private val geoService = GeoService()
    private val navigationService = NavigationService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }
    private val clipboard by lazy { Clipboard(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var direction1: Bearing? = null
    private var direction2: Bearing? = null
    private var location: Coordinate? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolTriangulateBinding.inflate(inflater, container, false)
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
                clipboard.copy(
                    formatService.formatLocation(it),
                    getString(R.string.copied_to_clipboard_toast)
                )
            }
        }

        binding.gpsOverrideBtn.setOnClickListener {
            location?.let { coord ->
                prefs.locationOverride = coord
                UiUtils.shortToast(requireContext(), getString(R.string.location_override_updated))
            }
        }

        binding.triangulate1.setOnCoordinateChangeListener { update() }
        binding.triangulate2.setOnCoordinateChangeListener { update() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        val c1 = binding.triangulate1.coordinate ?: return
        val c2 = binding.triangulate2.coordinate ?: return
        val d1 = direction1 ?: return
        val d2 = direction2 ?: return

        // All information is available to triangulate
        val declination = geoService.getDeclination(c1)
        val bearing1 = d1.withDeclination(declination)
        val bearing2 = d2.withDeclination(declination)

        location = navigationService.triangulate(c1, bearing1, c2, bearing2)

        if (location == null || location!!.latitude.isNaN() || location!!.longitude.isNaN()) {
            binding.location.text = getString(R.string.could_not_triangulate)
            binding.copyLocation.visibility = View.INVISIBLE
            binding.gpsOverrideBtn.visibility = View.INVISIBLE
        } else {
            binding.location.text = formatService.formatLocation(location!!)
            binding.copyLocation.visibility = View.VISIBLE
            if (!prefs.useAutoLocation) {
                binding.gpsOverrideBtn.visibility = View.VISIBLE
            }
        }
    }


}
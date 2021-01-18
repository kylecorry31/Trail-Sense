package com.kylecorry.trail_sense.tools.triangulate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolTriangulateBinding
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.NavigationService
import com.kylecorry.trailsensecore.infrastructure.persistence.Clipboard
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentToolTriangulate : Fragment() {

    private var _binding: FragmentToolTriangulateBinding? = null
    private val binding get() = _binding!!

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensorService.getCompass() }
    private val geoService = GeoService()
    private val navigationService = NavigationService()
    private val formatService by lazy { FormatService(requireContext()) }
    private val clipboard by lazy { Clipboard(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var beacon1: Beacon? = null
    private var beacon2: Beacon? = null
    private var direction1: Bearing? = null
    private var direction2: Bearing? = null
    private var location: Coordinate? = null

    private lateinit var beacons: List<Beacon>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolTriangulateBinding.inflate(inflater, container, false)
        binding.beacon1Compass.setOnClickListener {
            direction1 = compass.bearing
            binding.beacon1Direction.text = formatService.formatDegrees(compass.bearing.value)
            update()
        }
        binding.beacon2Compass.setOnClickListener {
            direction2 = compass.bearing
            binding.beacon2Direction.text = formatService.formatDegrees(compass.bearing.value)
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

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                beacons = beaconRepo.getBeaconsSync().map { it.toBeacon() }.sortedBy { it.name }.toList()
            }

            withContext(Dispatchers.Main){
                val adapter1 = ArrayAdapter(
                    requireContext(),
                    R.layout.beacon_spinner_item,
                    R.id.beacon_name,
                    beacons.map { it.name })
                binding.beacon1Spinner.prompt = getString(R.string.beacon_1)
                binding.beacon1Spinner.adapter = adapter1

                binding.beacon1Spinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            beacon1 = beacons[position]
                            update()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            beacon1 = null
                            update()
                        }
                    }

                val adapter2 = ArrayAdapter(
                    requireContext(),
                    R.layout.beacon_spinner_item,
                    R.id.beacon_name,
                    beacons.map { it.name })
                binding.beacon2Spinner.prompt = getString(R.string.beacon_2)
                binding.beacon2Spinner.adapter = adapter2

                binding.beacon2Spinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            beacon2 = beacons[position]
                            update()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            beacon2 = null
                            update()
                        }
                    }
            }
        }

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
        return true
    }

    private fun update() {
        val b1 = beacon1 ?: return
        val b2 = beacon2 ?: return
        val d1 = direction1 ?: return
        val d2 = direction2 ?: return

        // All information is available to triangulate
        val declination = geoService.getDeclination(b1.coordinate, b1.elevation)
        val bearing1 = d1.withDeclination(declination)
        val bearing2 = d2.withDeclination(declination)

        location = navigationService.triangulate(b1.coordinate, bearing1, b2.coordinate, bearing2)

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
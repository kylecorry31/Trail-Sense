package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateBeaconBinding
import com.kylecorry.trail_sense.navigation.domain.BeaconEntity
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.GeoService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PlaceBeaconFragment : Fragment() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private lateinit var navController: NavController

    private var _binding: FragmentCreateBeaconBinding? = null
    private val binding get() = _binding!!
    private val altimeter by lazy { sensorService.getAltimeter() }

    private lateinit var units: UserPreferences.DistanceUnits
    private val sensorService by lazy { SensorService(requireContext()) }
    private val compass by lazy { sensorService.getCompass() }
    private val formatService by lazy { FormatService(requireContext()) }

    private lateinit var groups: List<BeaconGroup>

    private var editingBeacon: Beacon? = null
    private var initialGroup: BeaconGroup? = null
    private var initialLocation: MyNamedCoordinate? = null
    private val geoService = GeoService()

    private var bearingTo: Bearing? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("edit_beacon") ?: 0L
        val groupId = arguments?.getLong("initial_group") ?: 0L
        initialLocation = arguments?.getParcelable("initial_location")

        editingBeacon = if (beaconId == 0L) {
            null
        } else {
            runBlocking {
                withContext(Dispatchers.IO) {
                    beaconRepo.getBeacon(beaconId)?.toBeacon()
                }
            }
        }

        initialGroup = if (groupId == 0L) {
            null
        } else {
            runBlocking {
                withContext(Dispatchers.IO) {
                    beaconRepo.getGroup(groupId)?.toBeaconGroup()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateBeaconBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = UserPreferences(requireContext())
        units = prefs.distanceUnits

        navController = findNavController()

        runBlocking {
            withContext(Dispatchers.IO) {
                groups =
                    listOf(
                        BeaconGroup(
                            0,
                            getString(R.string.no_group)
                        )
                    ) + beaconRepo.getGroupsSync().map { it.toBeaconGroup() }
                        .sortedBy { it.name }
            }
        }
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.beacon_group_spinner_item,
            R.id.beacon_group_name,
            groups.map { it.name })
        binding.beaconGroupSpinner.prompt = getString(R.string.beacon_group_spinner_title)
        binding.beaconGroupSpinner.adapter = adapter
        val idx = if (editingBeacon?.beaconGroupId != null) {
            val g = groups.find { it.id == editingBeacon?.beaconGroupId }
            if (g == null) {
                0
            } else {
                groups.indexOf(g)
            }
        } else if (initialGroup != null) {
            val i = groups.indexOf(initialGroup)
            if (i == -1) {
                0
            } else {
                i
            }
        } else {
            0
        }

        binding.beaconGroupSpinner.setSelection(idx)

        if (initialLocation != null) {
            binding.beaconName.setText(initialLocation!!.name ?: "")
            binding.beaconLocation.coordinate = initialLocation!!.coordinate
            updateDoneButtonState()
        }

        if (editingBeacon != null) {
            binding.beaconName.setText(editingBeacon?.name)
            binding.beaconLocation.coordinate = editingBeacon!!.coordinate
            binding.beaconElevation.setText(editingBeacon?.elevation?.toString() ?: "")
            binding.comment.setText(editingBeacon?.comment ?: "")
            updateDoneButtonState()
        }

        binding.beaconName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidName()) {
                binding.beaconName.error = getString(R.string.beacon_invalid_name)
            } else if (!hasFocus) {
                binding.beaconName.error = null
            }
        }

        binding.beaconLocation.setOnAutoLocationClickListener {
            binding.beaconElevation.isEnabled = false
            altimeter.start(this::setElevationFromAltimeter)
        }

        binding.beaconLocation.setOnCoordinateChangeListener {
            updateDoneButtonState()
        }

        binding.beaconElevation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidElevation()) {
                binding.beaconElevation.error = getString(R.string.beacon_invalid_elevation)
            } else if (!hasFocus) {
                binding.beaconElevation.error = null
            }
        }

        binding.beaconName.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.beaconElevation.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.createAtDistance.setOnCheckedChangeListener { _, isChecked ->
            binding.distanceAway.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.bearingToHolder.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateDoneButtonState()
        }

        binding.distanceAway.setOnDistanceChangeListener {
            updateDoneButtonState()
        }

        binding.bearingToBtn.setOnClickListener {
            bearingTo = compass.bearing
            binding.bearingTo.text = formatService.formatDegrees(bearingTo?.value ?: 0f)
            updateDoneButtonState()
        }

        binding.placeBeaconBtn.setOnClickListener {
            val name = binding.beaconName.text.toString()
            val createAtDistance = binding.createAtDistance.isChecked
            val distanceTo =
                binding.distanceAway.distance?.convertTo(DistanceUnits.Meters)?.distance?.toDouble()
                    ?: 0.0
            val bearingTo = bearingTo ?: Bearing.from(CompassDirection.North)
            val comment = binding.comment.text.toString()
            val rawElevation = binding.beaconElevation.text.toString().toFloatOrNull()
            val elevation = if (rawElevation == null) {
                null
            } else {
                LocationMath.convertToMeters(rawElevation, units)
            }

            val coordinate = if (createAtDistance) {
                val coord = binding.beaconLocation.coordinate
                val declination = if (coord != null) {
                    geoService.getDeclination(coord, elevation)
                } else {
                    0f
                }
                coord?.plus(distanceTo, bearingTo.withDeclination(declination))
            } else {
                binding.beaconLocation.coordinate
            }

            if (name.isNotBlank() && coordinate != null) {
                val groupId = when (binding.beaconGroupSpinner.selectedItemPosition) {
                    in 1 until groups.size -> {
                        groups[binding.beaconGroupSpinner.selectedItemPosition].id
                    }
                    else -> {
                        null
                    }
                }
                val beacon = if (editingBeacon == null) {
                    Beacon(0, name, coordinate, true, comment, groupId, elevation)
                } else {
                    Beacon(
                        editingBeacon!!.id,
                        name,
                        coordinate,
                        editingBeacon!!.visible,
                        comment,
                        groupId,
                        elevation
                    )
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.IO){
                        beaconRepo.addBeacon(BeaconEntity.from(beacon))
                    }

                    withContext(Dispatchers.Main){
                        if (initialLocation != null) {
                            requireActivity().onBackPressed()
                        } else {
                            navController.navigate(R.id.action_place_beacon_to_beacon_list)
                        }
                    }
                }
            }
        }

        if (units == UserPreferences.DistanceUnits.Feet) {
            binding.beaconElevation.hint = getString(R.string.beacon_elevation_hint_feet)
        } else {
            binding.beaconElevation.hint = getString(R.string.beacon_elevation_hint_meters)
        }

        binding.bearingToBtn.text =
            getString(R.string.beacon_set_bearing_btn, formatService.formatDegrees(0f))
        binding.distanceAway.units = listOf(
            DistanceUnits.Meters,
            DistanceUnits.Kilometers,
            DistanceUnits.Feet,
            DistanceUnits.Miles,
            DistanceUnits.NauticalMiles
        )
    }

    override fun onResume() {
        super.onResume()
        compass.start(this::onCompassUpdate)
    }

    override fun onPause() {
        compass.stop(this::onCompassUpdate)
        altimeter.stop(this::setElevationFromAltimeter)
        binding.beaconElevation.isEnabled = true
        super.onPause()
    }

    private fun onCompassUpdate(): Boolean {
        binding.bearingToBtn.text = getString(
            R.string.beacon_set_bearing_btn,
            formatService.formatDegrees(compass.bearing.value)
        )
        return true
    }

    private fun setElevationFromAltimeter(): Boolean {
        binding.beaconElevation.isEnabled = true
        if (units == UserPreferences.DistanceUnits.Meters) {
            binding.beaconElevation.setText(altimeter.altitude.roundPlaces(1).toString())
        } else {
            binding.beaconElevation.setText(
                LocationMath.convertToBaseUnit(altimeter.altitude, units).roundPlaces(1).toString()
            )
        }
        return false
    }

    private fun updateDoneButtonState() {
        binding.placeBeaconBtn.visibility =
            if (hasValidName() && binding.beaconLocation.coordinate != null && hasValidElevation() && hasValidDistanceTo()) View.VISIBLE else View.GONE
    }

    private fun hasValidDistanceTo(): Boolean {
        if (!binding.createAtDistance.isChecked) {
            return true
        }

        if (binding.distanceAway.distance == null) {
            return false
        }

        return bearingTo != null
    }


    private fun hasValidElevation(): Boolean {
        return binding.beaconElevation.text.isNullOrBlank() || binding.beaconElevation.text.toString()
            .toFloatOrNull() != null
    }

    private fun hasValidName(): Boolean {
        return !binding.beaconName.text.toString().isBlank()
    }

}
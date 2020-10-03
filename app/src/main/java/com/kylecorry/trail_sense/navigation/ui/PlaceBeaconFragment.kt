package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentCreateBeaconBinding
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup

class PlaceBeaconFragment : Fragment() {

    private val beaconRepo by lazy { BeaconRepo(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private lateinit var navController: NavController

    private lateinit var binding: FragmentCreateBeaconBinding
    private val altimeter by lazy { sensorService.getAltimeter() }

    private lateinit var units: UserPreferences.DistanceUnits
    private val sensorService by lazy { SensorService(requireContext()) }

    private lateinit var groups: List<BeaconGroup>

    private var editingBeacon: Beacon? = null
    private var initialGroup: BeaconGroup? = null
    private var initialLocation: MyNamedCoordinate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("edit_beacon") ?: 0L
        val groupId = arguments?.getLong("initial_group") ?: 0L
        initialLocation = arguments?.getParcelable("initial_location")

        editingBeacon = if (beaconId == 0L) {
            null
        } else {
            beaconRepo.get(beaconId)
        }

        initialGroup = if (groupId == 0L) {
            null
        } else {
            beaconRepo.getGroup(groupId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateBeaconBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = UserPreferences(requireContext())
        units = prefs.distanceUnits

        navController = findNavController()

        groups = listOf(BeaconGroup(0, getString(R.string.no_group))) + beaconRepo.getGroups()
            .sortedBy { it.name }
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
            binding.beaconLatitude.setText(initialLocation!!.coordinate.latitude.toString())
            binding.beaconLongitude.setText(initialLocation!!.coordinate.longitude.toString())
            updateDoneButtonState()
        }

        if (editingBeacon != null) {
            binding.beaconName.setText(editingBeacon?.name)
            binding.beaconLatitude.setText(editingBeacon?.coordinate?.latitude.toString())
            binding.beaconLongitude.setText(editingBeacon?.coordinate?.longitude.toString())
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

        binding.beaconLatitude.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidLatitude()) {
                binding.beaconLatitude.error = getString(R.string.beacon_invalid_latitude)
            } else if (!hasFocus) {
                binding.beaconLatitude.error = null
            }
        }

        binding.beaconLongitude.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidLongitude()) {
                binding.beaconLongitude.error = getString(R.string.beacon_invalid_longitude)
            } else if (!hasFocus) {
                binding.beaconLongitude.error = null
            }
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

        binding.beaconLatitude.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.beaconLongitude.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.beaconElevation.addTextChangedListener {
            updateDoneButtonState()
        }

        binding.placeBeaconBtn.setOnClickListener {
            val name = binding.beaconName.text.toString()
            val lat = binding.beaconLatitude.text.toString()
            val lng = binding.beaconLongitude.text.toString()
            val comment = binding.comment.text.toString()
            val rawElevation = binding.beaconElevation.text.toString().toFloatOrNull()
            val elevation = if (rawElevation == null) {
                null
            } else {
                LocationMath.convertToMeters(rawElevation, units)
            }

            val coordinate = getCoordinate(lat, lng)

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
                beaconRepo.add(beacon)
                if (initialLocation != null) {
                    requireActivity().onBackPressed()
                } else {
                    navController.navigate(R.id.action_place_beacon_to_beacon_list)
                }
            }
        }

        if (units == UserPreferences.DistanceUnits.Feet) {
            binding.beaconElevation.hint = getString(R.string.beacon_elevation_hint_feet)
        } else {
            binding.beaconElevation.hint = getString(R.string.beacon_elevation_hint_meters)
        }

        binding.currentLocationBtn.setOnClickListener {
            gps.start(this::setLocationFromGPS)
            altimeter.start(this::setElevationFromAltimeter)
        }

    }

    override fun onPause() {
        gps.stop(this::setLocationFromGPS)
        altimeter.stop(this::setElevationFromAltimeter)
        super.onPause()
    }

    private fun setElevationFromAltimeter(): Boolean {
        if (units == UserPreferences.DistanceUnits.Meters) {
            binding.beaconElevation.setText(altimeter.altitude.roundPlaces(1).toString())
        } else {
            binding.beaconElevation.setText(
                LocationMath.convertToBaseUnit(altimeter.altitude, units).roundPlaces(1).toString()
            )
        }
        return false
    }

    private fun setLocationFromGPS(): Boolean {
        binding.beaconLatitude.setText(gps.location.latitude.toString())
        binding.beaconLongitude.setText(gps.location.longitude.toString())
        return false
    }

    private fun updateDoneButtonState() {
        binding.placeBeaconBtn.visibility =
            if (hasValidName() && hasValidLatitude() && hasValidLongitude() && hasValidElevation()) View.VISIBLE else View.GONE
    }

    private fun hasValidLatitude(): Boolean {
        return Coordinate.parseLatitude(binding.beaconLatitude.text.toString()) != null
    }

    private fun hasValidLongitude(): Boolean {
        return Coordinate.parseLongitude(binding.beaconLongitude.text.toString()) != null
    }

    private fun hasValidElevation(): Boolean {
        return binding.beaconElevation.text.isNullOrBlank() || binding.beaconElevation.text.toString()
            .toFloatOrNull() != null
    }

    private fun hasValidName(): Boolean {
        return !binding.beaconName.text.toString().isBlank()
    }

    private fun getCoordinate(lat: String, lon: String): Coordinate? {
        val latitude = Coordinate.parseLatitude(lat)
        val longitude = Coordinate.parseLongitude(lon)

        if (latitude == null || longitude == null) {
            return null
        }

        return Coordinate(
            latitude,
            longitude
        )
    }

}
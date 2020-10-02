package com.kylecorry.trail_sense.navigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.MyNamedCoordinate
import com.kylecorry.trail_sense.navigation.infrastructure.database.BeaconRepo
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.infrastructure.system.GeoUriParser


class PlaceBeaconFragment : Fragment() {

    private val beaconRepo by lazy { BeaconRepo(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private lateinit var navController: NavController

    private lateinit var beaconName: EditText
    private lateinit var beaconLat: EditText
    private lateinit var beaconLng: EditText
    private lateinit var beaconElevation: EditText
    private lateinit var commentTxt: EditText
    private lateinit var useCurrentLocationBtn: Button
    private lateinit var doneBtn: FloatingActionButton
    private lateinit var groupSpinner: Spinner
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
        return inflater.inflate(R.layout.fragment_create_beacon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = UserPreferences(requireContext())
        units = prefs.distanceUnits

        beaconName = view.findViewById(R.id.beacon_name)
        beaconLat = view.findViewById(R.id.beacon_latitude)
        beaconLng = view.findViewById(R.id.beacon_longitude)
        beaconElevation = view.findViewById(R.id.beacon_elevation)
        commentTxt = view.findViewById(R.id.comment)
        useCurrentLocationBtn = view.findViewById(R.id.current_location_btn)
        groupSpinner = view.findViewById(R.id.beacon_group_spinner)
        doneBtn = view.findViewById(R.id.place_beacon_btn)
        navController = findNavController()

        groups = listOf(BeaconGroup(0, getString(R.string.no_group))) + beaconRepo.getGroups()
            .sortedBy { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.beacon_group_spinner_item,
            R.id.beacon_group_name,
            groups.map { it.name })
        groupSpinner.prompt = getString(R.string.beacon_group_spinner_title)
        groupSpinner.adapter = adapter
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

        groupSpinner.setSelection(idx)

        if (initialLocation != null) {
            beaconName.setText(initialLocation!!.name ?: "")
            beaconLat.setText(initialLocation!!.coordinate.latitude.toString())
            beaconLng.setText(initialLocation!!.coordinate.longitude.toString())
            updateDoneButtonState()
        }

        if (editingBeacon != null) {
            beaconName.setText(editingBeacon?.name)
            beaconLat.setText(editingBeacon?.coordinate?.latitude.toString())
            beaconLng.setText(editingBeacon?.coordinate?.longitude.toString())
            beaconElevation.setText(editingBeacon?.elevation?.toString() ?: "")
            commentTxt.setText(editingBeacon?.comment ?: "")
            updateDoneButtonState()
        }

        beaconName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidName()) {
                beaconName.error = getString(R.string.beacon_invalid_name)
            } else if (!hasFocus) {
                beaconName.error = null
            }
        }

        beaconLat.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidLatitude()) {
                beaconLat.error = getString(R.string.beacon_invalid_latitude)
            } else if (!hasFocus) {
                beaconLat.error = null
            }
        }

        beaconLng.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidLongitude()) {
                beaconLng.error = getString(R.string.beacon_invalid_longitude)
            } else if (!hasFocus) {
                beaconLng.error = null
            }
        }

        beaconElevation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidElevation()) {
                beaconElevation.error = getString(R.string.beacon_invalid_elevation)
            } else if (!hasFocus) {
                beaconElevation.error = null
            }
        }

        beaconName.addTextChangedListener {
            updateDoneButtonState()
        }

        beaconLat.addTextChangedListener {
            updateDoneButtonState()
        }

        beaconLng.addTextChangedListener {
            updateDoneButtonState()
        }

        beaconElevation.addTextChangedListener {
            updateDoneButtonState()
        }

        doneBtn.setOnClickListener {
            val name = beaconName.text.toString()
            val lat = beaconLat.text.toString()
            val lng = beaconLng.text.toString()
            val comment = commentTxt.text.toString()
            val rawElevation = beaconElevation.text.toString().toFloatOrNull()
            val elevation = if (rawElevation == null) {
                null
            } else {
                LocationMath.convertToMeters(rawElevation, units)
            }

            val coordinate = getCoordinate(lat, lng)

            if (name.isNotBlank() && coordinate != null) {
                val groupId = when (groupSpinner.selectedItemPosition) {
                    in 1 until groups.size -> {
                        groups[groupSpinner.selectedItemPosition].id
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
                if (initialLocation != null){
                    requireActivity().onBackPressed()
                } else {
                    navController.navigate(R.id.action_place_beacon_to_beacon_list)
                }
            }
        }

        if (units == UserPreferences.DistanceUnits.Feet) {
            beaconElevation.hint = getString(R.string.beacon_elevation_hint_feet)
        } else {
            beaconElevation.hint = getString(R.string.beacon_elevation_hint_meters)
        }

        useCurrentLocationBtn.setOnClickListener {
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
            beaconElevation.setText(altimeter.altitude.roundPlaces(1).toString())
        } else {
            beaconElevation.setText(
                LocationMath.convertToBaseUnit(altimeter.altitude, units).roundPlaces(1).toString()
            )
        }
        return false
    }

    private fun setLocationFromGPS(): Boolean {
        beaconLat.setText(gps.location.latitude.toString())
        beaconLng.setText(gps.location.longitude.toString())
        return false
    }

    private fun updateDoneButtonState() {
        doneBtn.visibility =
            if (hasValidName() && hasValidLatitude() && hasValidLongitude() && hasValidElevation()) View.VISIBLE else View.GONE
    }

    private fun hasValidLatitude(): Boolean {
        return Coordinate.parseLatitude(beaconLat.text.toString()) != null
    }

    private fun hasValidLongitude(): Boolean {
        return Coordinate.parseLongitude(beaconLng.text.toString()) != null
    }

    private fun hasValidElevation(): Boolean {
        return beaconElevation.text.isNullOrBlank() || beaconElevation.text.toString()
            .toFloatOrNull() != null
    }

    private fun hasValidName(): Boolean {
        return !beaconName.text.toString().isBlank()
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
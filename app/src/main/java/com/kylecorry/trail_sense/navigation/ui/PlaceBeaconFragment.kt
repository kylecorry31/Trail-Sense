package com.kylecorry.trail_sense.navigation.ui

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconRepo
import com.kylecorry.trail_sense.navigation.infrastructure.GeoUriParser
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IGPS


class PlaceBeaconFragment(
    private val _repo: BeaconRepo?,
    private val _gps: IGPS?,
    private val initialLocation: GeoUriParser.NamedCoordinate? = null,
    private val editingBeacon: Beacon? = null
) : Fragment() {

    private lateinit var beaconRepo: BeaconRepo
    private lateinit var gps: IGPS

    constructor(): this(null, null, null)

    private lateinit var beaconName: EditText
    private lateinit var beaconLat: EditText
    private lateinit var beaconLng: EditText
    private lateinit var beaconElevation: EditText
    private lateinit var commentTxt: EditText
    private lateinit var useCurrentLocationBtn: Button
    private lateinit var doneBtn: FloatingActionButton

    private lateinit var units: UserPreferences.DistanceUnits

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_beacon, container, false)

        beaconRepo = _repo ?: BeaconRepo(requireContext())
        gps = _gps ?: GPS(requireContext())

        val prefs = UserPreferences(requireContext())
        units = prefs.distanceUnits

        beaconName = view.findViewById(R.id.beacon_name)
        beaconLat = view.findViewById(R.id.beacon_latitude)
        beaconLng = view.findViewById(R.id.beacon_longitude)
        beaconElevation = view.findViewById(R.id.beacon_elevation)
        commentTxt = view.findViewById(R.id.comment)
        useCurrentLocationBtn = view.findViewById(R.id.current_location_btn)
        doneBtn = view.findViewById(R.id.place_beacon_btn)

        if (initialLocation != null) {
            beaconName.setText(initialLocation.name ?: "")
            beaconLat.setText(initialLocation.coordinate.latitude.toString())
            beaconLng.setText(initialLocation.coordinate.longitude.toString())
            updateDoneButtonState()
        }

        if (editingBeacon != null) {
            beaconName.setText(editingBeacon.name)
            beaconLat.setText(editingBeacon.coordinate.latitude.toString())
            beaconLng.setText(editingBeacon.coordinate.longitude.toString())
            beaconElevation.setText(editingBeacon.elevation?.toString() ?: "")
            commentTxt.setText(editingBeacon.comment ?: "")
            updateDoneButtonState()
        }

        beaconName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidName()) {
                beaconName.error = "Invalid beacon name"
            } else if (!hasFocus) {
                beaconName.error = null
            }
        }

        beaconLat.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidLatitude()) {
                beaconLat.error = "Invalid latitude"
            } else if (!hasFocus) {
                beaconLat.error = null
            }
        }

        beaconLng.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidLongitude()) {
                beaconLng.error = "Invalid longitude"
            } else if (!hasFocus) {
                beaconLng.error = null
            }
        }

        beaconElevation.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !hasValidElevation()) {
                beaconElevation.error = "Invalid elevation"
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
            val elevation = if (rawElevation == null){
                null
            } else {
                LocationMath.convertToMeters(rawElevation, units)
            }

            val coordinate = getCoordinate(lat, lng)

            if (name.isNotBlank() && coordinate != null) {
                val beacon = if (editingBeacon == null ) {
                    Beacon(0, name, coordinate, true, comment, null, elevation)
                } else {
                    Beacon(editingBeacon.id, name, coordinate, editingBeacon.visible, comment, editingBeacon.beaconGroupId, elevation)
                }
                beaconRepo.add(beacon)
                parentFragmentManager.doTransaction {
                    this.replace(
                        R.id.fragment_holder,
                        BeaconListFragment(
                            beaconRepo,
                            gps
                        )
                    )
                }
            }
        }

        if (units == UserPreferences.DistanceUnits.Feet){
            beaconElevation.hint = getString(R.string.beacon_elevation_hint_feet)
        } else {
            beaconElevation.hint = getString(R.string.beacon_elevation_hint_meters)
        }

        useCurrentLocationBtn.setOnClickListener {
            gps.start(this::setLocationFromGPS)
        }

        return view
    }

    private fun setLocationFromGPS(): Boolean {
        beaconLat.setText(gps.location.latitude.toString())
        beaconLng.setText(gps.location.longitude.toString())

        if (units == UserPreferences.DistanceUnits.Meters) {
            beaconElevation.setText(gps.altitude.roundPlaces(1).toString())
        } else {
            beaconElevation.setText(LocationMath.convertToBaseUnit(gps.altitude, units).roundPlaces(1).toString())
        }
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
        return beaconElevation.text.isNullOrBlank() || beaconElevation.text.toString().toFloatOrNull() != null
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
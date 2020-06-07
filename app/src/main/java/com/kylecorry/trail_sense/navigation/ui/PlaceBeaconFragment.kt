package com.kylecorry.trail_sense.navigation.ui

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
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconDB
import com.kylecorry.trail_sense.navigation.infrastructure.GeoUriParser
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.sensors.GPS
import com.kylecorry.trail_sense.shared.sensors.IGPS


class PlaceBeaconFragment(
    private val _beaconDB: BeaconDB?,
    private val _gps: IGPS?,
    private val initialLocation: GeoUriParser.NamedCoordinate? = null
) : Fragment() {

    private lateinit var beaconDB: BeaconDB
    private lateinit var gps: IGPS

    constructor(): this(null, null, null)

    private lateinit var beaconName: EditText
    private lateinit var beaconLat: EditText
    private lateinit var beaconLng: EditText
    private lateinit var useCurrentLocationBtn: Button
    private lateinit var doneBtn: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_beacon, container, false)

        beaconDB = _beaconDB ?: BeaconDB(requireContext())
        gps = _gps ?: GPS(requireContext())

        beaconName = view.findViewById(R.id.beacon_name)
        beaconLat = view.findViewById(R.id.beacon_latitude)
        beaconLng = view.findViewById(R.id.beacon_longitude)
        useCurrentLocationBtn = view.findViewById(R.id.current_location_btn)
        doneBtn = view.findViewById(R.id.place_beacon_btn)

        if (initialLocation != null) {
            beaconName.setText(initialLocation.name ?: "")
            beaconLat.setText(initialLocation.coordinate.latitude.toString())
            beaconLng.setText(initialLocation.coordinate.longitude.toString())
        }

        beaconName.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && !hasValidName()) {
                beaconName.error = "Invalid beacon name"
            } else if (!hasFocus) {
                beaconName.error = null
            }
        }

        beaconLat.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && !hasValidLatitude()) {
                beaconLat.error = "Invalid latitude"
            } else if (!hasFocus) {
                beaconLat.error = null
            }
        }

        beaconLng.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && !hasValidLongitude()) {
                beaconLng.error = "Invalid longitude"
            } else if (!hasFocus) {
                beaconLng.error = null
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

        doneBtn.setOnClickListener {
            val name = beaconName.text.toString()
            val lat = beaconLat.text.toString()
            val lng = beaconLng.text.toString()

            val coordinate = getCoordinate(lat, lng)

            if (name.isNotBlank() && coordinate != null) {
                val beacon = Beacon(name, coordinate)
                beaconDB.create(beacon)
                parentFragmentManager.doTransaction {
                    this.replace(
                        R.id.fragment_holder,
                        BeaconListFragment(
                            beaconDB,
                            gps
                        )
                    )
                }
            }
        }


        useCurrentLocationBtn.setOnClickListener {
            gps.start(this::setLocationFromGPS)
        }

        return view
    }

    private fun setLocationFromGPS(): Boolean {
        beaconLat.setText(gps.location.latitude.toString())
        beaconLng.setText(gps.location.longitude.toString())
        return false
    }

    private fun updateDoneButtonState() {
        doneBtn.visibility =
            if (hasValidName() && hasValidLatitude() && hasValidLongitude()) View.VISIBLE else View.GONE
    }

    private fun hasValidLatitude(): Boolean {
        return Coordinate.parseLatitude(beaconLat.text.toString()) != null
    }

    private fun hasValidLongitude(): Boolean {
        return Coordinate.parseLongitude(beaconLng.text.toString()) != null
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
package com.kylecorry.trail_sense.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.Beacon
import com.kylecorry.trail_sense.navigation.BeaconDB
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.sensors.gps.GPS


class PlaceBeaconFragment(private val beaconDB: BeaconDB, private val gps: GPS): Fragment() {

    private lateinit var beaconName: EditText
    private lateinit var beaconLat: EditText
    private lateinit var beaconLng: EditText
    private lateinit var useCurrentLocationBtn: Button
    private lateinit var doneBtn: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_create_beacon, container, false)

        beaconName = view.findViewById(R.id.beacon_name)
        beaconLat = view.findViewById(R.id.beacon_latitude)
        beaconLng = view.findViewById(R.id.beacon_longitude)
        useCurrentLocationBtn = view.findViewById(R.id.current_location_btn)
        doneBtn = view.findViewById(R.id.place_beacon_btn)

        doneBtn.setOnClickListener {
            val name = beaconName.text.toString()
            val lat = beaconLat.text.toString().toDoubleOrNull()
            val lng = beaconLng.text.toString().toDoubleOrNull()

            if (name.isNotBlank() && lat != null && lng != null){
                // All fields supplied, create the beacon
                val beacon = Beacon(
                    name,
                    Coordinate(lat, lng)
                )
                beaconDB.create(beacon)
                fragmentManager?.doTransaction {
                    this.replace(R.id.fragment_holder,
                        BeaconListFragment(
                            beaconDB,
                            gps
                        )
                    )
                }
            }
        }


        useCurrentLocationBtn.setOnClickListener {
            gps.updateLocation {
                if (it != null) {
                    beaconLat.setText(it.latitude.toString())
                    beaconLng.setText(it.longitude.toString())
                }
            }
        }

        return view
    }

}
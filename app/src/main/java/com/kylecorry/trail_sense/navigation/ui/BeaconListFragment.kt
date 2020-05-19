package com.kylecorry.trail_sense.navigation.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconDB
import com.kylecorry.trail_sense.navigation.infrastructure.LocationSharesheet
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.doTransaction
import com.kylecorry.trail_sense.shared.sensors.IGPS


class BeaconListFragment(private val beaconDB: BeaconDB, private val gps: IGPS): Fragment() {

    private lateinit var beaconList: RecyclerView
    private lateinit var createBtn: FloatingActionButton
    private lateinit var adapter: BeaconAdapter
    private lateinit var emptyTxt: TextView
    private lateinit var prefs: UserPreferences
    private val location = gps.location

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_beacon_list, container, false)

        beaconList = view.findViewById(R.id.beacon_recycler)
        createBtn = view.findViewById(R.id.create_beacon_btn)
        emptyTxt = view.findViewById(R.id.beacon_empty_text)

        prefs = UserPreferences(requireContext())

        val layoutManager = LinearLayoutManager(context)
        beaconList.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            context,
            layoutManager.orientation
        )
        beaconList.addItemDecoration(dividerItemDecoration)

        val beacons = beaconDB.beacons.sortedBy { it.coordinate.distanceTo(location) }
        updateBeaconEmptyText(beacons.isNotEmpty())

        adapter = BeaconAdapter(beacons)
        beaconList.adapter = adapter

        createBtn.setOnClickListener {
            parentFragmentManager.doTransaction {
                this.replace(R.id.fragment_holder,
                    PlaceBeaconFragment(
                        beaconDB,
                        gps
                    )
                )
                addToBackStack(javaClass.name)
            }
        }

        return view
    }

    private fun updateBeaconEmptyText(hasBeacons: Boolean){
        if (!hasBeacons){
            emptyTxt.visibility = View.VISIBLE
        } else {
            emptyTxt.visibility = View.GONE
        }
    }

    inner class BeaconHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var nameText: TextView = itemView.findViewById(R.id.beacon_name_disp)
        private var locationText: TextView = itemView.findViewById(R.id.beacon_location_disp)
        private var distanceText: TextView = itemView.findViewById(R.id.beacon_distance_disp)
        private var copyBtn: ImageButton = itemView.findViewById(R.id.copy_btn)

        fun bindToBeacon(beacon: Beacon){
            nameText.text = beacon.name
            locationText.text = beacon.coordinate.getFormattedString()
            val distance = beacon.coordinate.distanceTo(location)
            distanceText.text = LocationMath.distanceToReadableString(distance, prefs.distanceUnits)

            copyBtn.setOnClickListener {
                val sender = LocationSharesheet(requireContext())
                sender.send(beacon.coordinate)
            }

            itemView.setOnClickListener {
                parentFragmentManager.doTransaction {
                    this.replace(R.id.fragment_holder,
                        NavigatorFragment(
                            beacon
                        )
                    )
                }
            }

            itemView.setOnLongClickListener {
                val dialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(R.string.dialog_ok) { _, _ ->
                            beaconDB.delete(beacon)
                            adapter.beacons = beaconDB.beacons.sortedBy { beacon -> beacon.coordinate.distanceTo(location) }
                            updateBeaconEmptyText(adapter.beacons.isNotEmpty())
                        }
                        setNegativeButton(R.string.dialog_cancel){ _, _ ->
                            // Do nothing
                        }
                        setMessage("Are you sure you want to remove \"${beacon.name}\"?")
                        setTitle(R.string.delete_beacon_alert_title)
                    }
                    builder.create()
                }
                dialog?.show()
                true
            }
        }
    }

    inner class BeaconAdapter(mBeacons: List<Beacon>): RecyclerView.Adapter<BeaconHolder>() {

        var beacons: List<Beacon> = mBeacons
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconHolder {
            val view = layoutInflater.inflate(R.layout.list_item_beacon, parent, false)
            return BeaconHolder(view)
        }

        override fun getItemCount(): Int {
            return beacons.size
        }

        override fun onBindViewHolder(holder: BeaconHolder, position: Int) {
            val beacon = beacons[position]
            holder.bindToBeacon(beacon)
        }

    }

}


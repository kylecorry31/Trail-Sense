package com.kylecorry.survival_aid.navigator.beacons

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.doTransaction
import com.kylecorry.survival_aid.navigator.gps.GPS
import com.kylecorry.survival_aid.navigator.NavigatorFragment
import kotlinx.android.synthetic.main.activity_navigator.*


class BeaconListFragment(private val beaconDB: BeaconDB, private val gps: GPS): Fragment() {

    private lateinit var beaconList: RecyclerView
    private lateinit var createBtn: FloatingActionButton
    private lateinit var adapter: BeaconAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_beacon_list, container, false)

        beaconList = view.findViewById(R.id.beacon_recycler)
        createBtn = view.findViewById(R.id.create_beacon_btn)

        beaconList.layoutManager = LinearLayoutManager(context)

        adapter = BeaconAdapter(beaconDB.beacons)
        beaconList.adapter = adapter

        createBtn.setOnClickListener {
            fragmentManager?.doTransaction {
                this.replace(R.id.fragment_holder,
                    PlaceBeaconFragment(beaconDB, gps)
                )
            }
        }

        return view
    }

    inner class BeaconHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var nameText: TextView = itemView.findViewById(R.id.beacon_name_disp)
        private var locationText: TextView = itemView.findViewById(R.id.beacon_location_disp)

        fun bindToBeacon(beacon: Beacon){
            nameText.text = beacon.name
            locationText.text = beacon.coordinate.toString()

            itemView.setOnClickListener {
                fragmentManager?.doTransaction {
                    this.replace(R.id.fragment_holder,
                        NavigatorFragment(beacon)
                    )
                }
            }

            itemView.setOnLongClickListener {
                val dialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(R.string.dialog_ok) { dialog, id ->
                            beaconDB.delete(beacon)
                            adapter.beacons = beaconDB.beacons
                        }
                        setNegativeButton(R.string.dialog_cancel){ dialog, id ->
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

